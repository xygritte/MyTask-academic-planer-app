from pathlib import Path

path = Path('app/src/main/java/com/mytask/MainActivity.kt')
text = path.read_text(encoding='utf-8')
old = '''        if (user == null) {\n            accountLoading = false\n            restorePendingState = false\n            if (sessionUid == "guest") syncReady = true\n            return@LaunchedEffect\n        }\n'''
new = '''        if (user == null) {\n            accountLoading = false\n            restorePendingState = false\n\n            if (sessionUid == "guest") {\n                syncReady = true\n            } else {\n                sessionProfile = null\n                sessionUid = null\n                syncReady = false\n                shouldShowTemplatePrompt = false\n                templateError = null\n                onlineSaveMessage = null\n            }\n            return@LaunchedEffect\n        }\n'''
if old not in text:
    raise SystemExit('Expected logout block was not found')
path.write_text(text.replace(old, new, 1), encoding='utf-8')
