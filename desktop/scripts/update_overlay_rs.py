path = r'c:\Users\Shiva\applications\vocawin\src-tauri\src\overlay.rs'
with open(path, 'r', encoding='utf-8') as f:
    text = f.read()

old = 'Phase::Error(message) => (\n            Payload {\n                phase: "error".into(),\n                title: "Dictation failed".into(),\n                detail: short(&message),'
new = 'Phase::Error(_message) => (\n            Payload {\n                phase: "error".into(),\n                title: "Dictation failed".into(),\n                detail: String::new(),'

if old in text:
    text = text.replace(old, new)
    print("Replaced LF version")
elif old.replace('\n', '\r\n') in text:
    text = text.replace(old.replace('\n', '\r\n'), new.replace('\n', '\r\n'))
    print("Replaced CRLF version")
else:
    print("Pattern not found!")

with open(path, 'w', encoding='utf-8') as f:
    f.write(text)
