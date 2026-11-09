path = r'c:\Users\Shiva\applications\vocawin\src-tauri\src\lib.rs'
with open(path, 'r', encoding='utf-8') as f:
    text = f.read()
replacements = {
    '"VocaWin - Unloaded to save RAM': '"Flow - Unloaded to save RAM',
    '"VocaWin - Paused because': '"Flow - Paused because',
    'ParkReason::None => "VocaWin".into()': 'ParkReason::None => "Flow".into()',
    '"VocaWin - Listening"': '"Flow - Listening"',
    '"VocaWin - Processing"': '"Flow - Processing"',
    '.tooltip("VocaWin")': '.tooltip("Flow")',
    '"error while running VocaWin"': '"error while running Flow"',
    'TrayPhase::Idle => (\n                "VocaWin".into()': 'TrayPhase::Idle => (\n                "Flow".into()',
    'TrayPhase::Idle => (\r\n                "VocaWin".into()': 'TrayPhase::Idle => (\r\n                "Flow".into()'
}
for old, new in replacements.items():
    text = text.replace(old, new)
with open(path, 'w', encoding='utf-8') as f:
    f.write(text)
print('Updated lib.rs branding successfully.')
