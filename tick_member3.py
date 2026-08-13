import re
paths = ['documents/phase_1_execution_playbook.md', 'documents/phase_1_execution_blueprint.md']
for p in paths:
    with open(p, 'r', encoding='utf-8') as f:
        content = f.read()
    content = re.sub(r'- \[ \] (Member 3: Profile Dashboards \(Mock\))', r'- [x] \1', content)
    content = re.sub(r'- \[ \] (Member 3: Dashboard Integration)', r'- [x] \1', content)
    content = re.sub(r'- \[ \] (Member 3: Leaderboard Integration)', r'- [x] \1', content)
    
    # Check if anything changed
    with open(p, 'w', encoding='utf-8') as f:
        f.write(content)
