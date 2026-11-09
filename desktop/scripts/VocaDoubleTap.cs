using System;
using System.Diagnostics;
using System.IO;
using System.Runtime.InteropServices;
using System.Threading;
using System.Windows.Forms;

namespace VocaDoubleTap
{
    static class Program
    {
        private const int WH_KEYBOARD_LL = 13;
        private const int WM_KEYDOWN = 0x0100;
        private const int WM_KEYUP = 0x0101;
        private const int WM_SYSKEYDOWN = 0x0104;
        private const int WM_SYSKEYUP = 0x0105;

        private const int VK_MENU = 0x12;
        private const int VK_RMENU = 0xA5;
        private const int VK_LCONTROL = 0xA2;
        private const int VK_F9 = 0x78;

        private const uint LLKHF_EXTENDED = 0x01;
        private const uint LLKHF_INJECTED = 0x10;

        private const uint KEYEVENTF_EXTENDEDKEY = 0x0001;
        private const uint KEYEVENTF_KEYUP = 0x0002;

        private static LowLevelKeyboardProc _proc = HookCallback;
        private static IntPtr _hookId = IntPtr.Zero;

        private static System.Threading.Timer _holdTimer;
        private static System.Threading.Timer _doubleTapTimer;

        private static bool _isPhysicallyDown = false;
        private static bool _isHoldActive = false;
        private static int _tapCount = 0;
        private static readonly object _lock = new object();

        private static string _logFile;

        private static void Log(string msg)
        {
            try
            {
                if (_logFile != null)
                {
                    File.AppendAllText(_logFile, string.Format("[{0:HH:mm:ss.fff}] {1}\r\n", DateTime.Now, msg));
                }
            }
            catch { }
        }

        [STAThread]
        static void Main()
        {
            string appData = Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData);
            string dir = Path.Combine(appData, "VocaWin");
            if (!Directory.Exists(dir)) Directory.CreateDirectory(dir);
            _logFile = Path.Combine(dir, "vocadoubletap.log");

            Log("Starting VocaDoubleTap service...");

            bool createdNew;
            using (var mutex = new Mutex(true, "Global\\VocaDoubleTap_SingleInstanceMutex", out createdNew))
            {
                if (!createdNew)
                {
                    Log("Another instance is already running. Exiting.");
                    return;
                }

                _holdTimer = new System.Threading.Timer(OnHoldTimeout, null, Timeout.Infinite, Timeout.Infinite);
                _doubleTapTimer = new System.Threading.Timer(OnDoubleTapTimeout, null, Timeout.Infinite, Timeout.Infinite);

                _hookId = SetHook(_proc);
                if (_hookId == IntPtr.Zero)
                {
                    Log("Failed to install keyboard hook. Error: " + Marshal.GetLastWin32Error());
                    return;
                }

                Log("Keyboard hook installed successfully. HookId: " + _hookId);

                // Run message loop
                Application.Run(new ApplicationContext());

                UnhookWindowsHookEx(_hookId);
                Log("VocaDoubleTap stopped.");
            }
        }

        private static IntPtr SetHook(LowLevelKeyboardProc proc)
        {
            using (Process curProcess = Process.GetCurrentProcess())
            using (ProcessModule curModule = curProcess.MainModule)
            {
                return SetWindowsHookEx(WH_KEYBOARD_LL, proc, GetModuleHandle(curModule.ModuleName), 0);
            }
        }

        private delegate IntPtr LowLevelKeyboardProc(int nCode, IntPtr wParam, IntPtr lParam);

        private static IntPtr HookCallback(int nCode, IntPtr wParam, IntPtr lParam)
        {
            if (nCode >= 0)
            {
                int msg = wParam.ToInt32();
                KBDLLHOOKSTRUCT kb = (KBDLLHOOKSTRUCT)Marshal.PtrToStructure(lParam, typeof(KBDLLHOOKSTRUCT));

                // If injected by our own process, let it pass to VocaWin
                if ((kb.flags & LLKHF_INJECTED) != 0)
                {
                    return CallNextHookEx(_hookId, nCode, wParam, lParam);
                }

                // Check for Right Alt (extended Alt or VK_RMENU)
                bool isRightAlt = (kb.vkCode == VK_RMENU) || (kb.vkCode == VK_MENU && (kb.flags & LLKHF_EXTENDED) != 0);

                if (isRightAlt)
                {
                    // If Left Ctrl is held, this is AltGr. Don't interfere.
                    if ((GetKeyState(VK_LCONTROL) & 0x8000) != 0)
                    {
                        return CallNextHookEx(_hookId, nCode, wParam, lParam);
                    }

                    if (msg == WM_KEYDOWN || msg == WM_SYSKEYDOWN)
                    {
                        lock (_lock)
                        {
                            if (!_isPhysicallyDown)
                            {
                                _isPhysicallyDown = true;

                                if (_tapCount == 1)
                                {
                                    // Second tap down: cancel the double-tap timeout
                                    Log("Right Alt second tap down");
                                    _doubleTapTimer.Change(Timeout.Infinite, Timeout.Infinite);
                                }
                                else
                                {
                                    // First tap or start of hold: arm the hold timer (200ms)
                                    Log("Right Alt down - hold timer started");
                                    _holdTimer.Change(200, Timeout.Infinite);
                                }
                            }
                        }
                        return (IntPtr)1; // Consume key down
                    }
                    else if (msg == WM_KEYUP || msg == WM_SYSKEYUP)
                    {
                        lock (_lock)
                        {
                            _isPhysicallyDown = false;
                            _holdTimer.Change(Timeout.Infinite, Timeout.Infinite);

                            if (_isHoldActive)
                            {
                                // User was holding Right Alt: release it to VocaWin to end push-to-talk
                                Log("Right Alt hold released - stopping push-to-talk");
                                _isHoldActive = false;
                                _tapCount = 0;
                                SendRightAlt(false);
                            }
                            else
                            {
                                // User released before hold threshold => it's a tap!
                                if (_tapCount == 0)
                                {
                                    Log("Right Alt first tap up - waiting for second tap");
                                    _tapCount = 1;
                                    // Wait up to 350ms for a second tap
                                    _doubleTapTimer.Change(350, Timeout.Infinite);
                                }
                                else if (_tapCount == 1)
                                {
                                    // Second tap released within window => DOUBLE TAP!
                                    Log("Right Alt DOUBLE TAP CONFIRMED! Sending F9 to VocaWin");
                                    _tapCount = 0;
                                    _doubleTapTimer.Change(Timeout.Infinite, Timeout.Infinite);
                                    // Trigger VocaWin Hands-Free shortcut (F9)
                                    SendF9();
                                }
                            }
                        }
                        return (IntPtr)1; // Consume key up
                    }
                }
            }

            return CallNextHookEx(_hookId, nCode, wParam, lParam);
        }

        private static void OnHoldTimeout(object state)
        {
            lock (_lock)
            {
                if (_isPhysicallyDown && !_isHoldActive)
                {
                    _isHoldActive = true;
                    _tapCount = 0;
                    _doubleTapTimer.Change(Timeout.Infinite, Timeout.Infinite);

                    Log("Hold threshold reached (200ms) - starting push-to-talk in VocaWin");
                    // Forward Right Alt Down to VocaWin to start push-to-talk
                    SendRightAlt(true);
                }
            }
        }

        private static void OnDoubleTapTimeout(object state)
        {
            lock (_lock)
            {
                if (_tapCount == 1 && !_isPhysicallyDown && !_isHoldActive)
                {
                    Log("Double-tap timeout expired with single tap");
                    _tapCount = 0;
                    // It was a single solitary tap: synthesize normal Alt keypress so menus work
                    SendRightAlt(true);
                    Thread.Sleep(10);
                    SendRightAlt(false);
                }
            }
        }

        private static void SendRightAlt(bool down)
        {
            uint flags = KEYEVENTF_EXTENDEDKEY;
            if (!down) flags |= KEYEVENTF_KEYUP;
            keybd_event((byte)VK_MENU, 0x38, flags, UIntPtr.Zero);
        }

        private static void SendF9()
        {
            keybd_event((byte)VK_F9, 0x43, 0, UIntPtr.Zero);
            Thread.Sleep(15);
            keybd_event((byte)VK_F9, 0x43, KEYEVENTF_KEYUP, UIntPtr.Zero);
        }

        [StructLayout(LayoutKind.Sequential)]
        private struct KBDLLHOOKSTRUCT
        {
            public uint vkCode;
            public uint scanCode;
            public uint flags;
            public uint time;
            public IntPtr dwExtraInfo;
        }

        [DllImport("user32.dll", CharSet = CharSet.Auto, SetLastError = true)]
        private static extern IntPtr SetWindowsHookEx(int idHook, LowLevelKeyboardProc lpfn, IntPtr hMod, uint dwThreadId);

        [DllImport("user32.dll", CharSet = CharSet.Auto, SetLastError = true)]
        [return: MarshalAs(UnmanagedType.Bool)]
        private static extern bool UnhookWindowsHookEx(IntPtr hhk);

        [DllImport("user32.dll", CharSet = CharSet.Auto, SetLastError = true)]
        private static extern IntPtr CallNextHookEx(IntPtr hhk, int nCode, IntPtr wParam, IntPtr lParam);

        [DllImport("kernel32.dll", CharSet = CharSet.Auto, SetLastError = true)]
        private static extern IntPtr GetModuleHandle(string lpModuleName);

        [DllImport("user32.dll")]
        private static extern short GetKeyState(int nVirtKey);

        [DllImport("user32.dll")]
        private static extern void keybd_event(byte bVk, byte bScan, uint dwFlags, UIntPtr dwExtraInfo);
    }
}
