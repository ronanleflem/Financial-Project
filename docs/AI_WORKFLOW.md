# FILE: docs/AI_WORKFLOW.md
# AI Workflow Guide (Spring local execution)

This file defines local execution rules for Spring only.

Global orchestration is managed in:
`C:\Users\ronan\Desktop\Cross-repo-coordination\Cross-repo-coordination`

## Local scope rules
- Keep scope local to Spring repository.
- Do not orchestrate Python or Angular from this file.
- Follow local ticket template and audit prompt.

## Local BMAD sequence
1. PM
2. Architect
3. Dev
4. Reviewer

## Context7
Use only when external docs are required.

## Validation
- `$env:JAVA_HOME="C:\Users\ronan\.jdks\corretto-21.0.4"`
- `$env:Path="$env:JAVA_HOME\bin;$env:Path"`
- `./mvnw test`
