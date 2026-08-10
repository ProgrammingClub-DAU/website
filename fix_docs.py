import os

files = [
    'documents/phase_1_execution_blueprint.md',
    'documents/phase_1_execution_playbook.md',
    'documents/team_roles.md',
    'documents/project_roadmap_all_phases.md'
]

for file in files:
    if os.path.exists(file):
        with open(file, 'r', encoding='utf-8') as f:
            content = f.read()
        
        content = content.replace('`backend/src/.../controller/`', '`backend/src/.../user/` (Feature Folder)')
        content = content.replace('`backend/src/.../service/CFSync.java`', '`backend/src/.../codeforces/CodeforcesSyncService.java`')
        content = content.replace('REST APIs, DTOs, Queries', 'REST APIs, Java Record DTOs, Queries')
        content = content.replace('REST APIs and DTOs.', 'REST APIs and DTOs (implemented as Java Records).')
        content = content.replace('DTOs implemented.', 'DTOs implemented as immutable Java Records.')
        content = content.replace('Database Queries, Business Logic, and DTOs', 'Database Queries, Business Logic, and Java Record DTOs')
        content = content.replace('Fetch `https://codeforces.com/api/user.info` for all users.', 'Fetch `https://codeforces.com/api/user.info` for all users (Note: Sync also occurs instantaneously when a user updates their handle).')
        
        with open(file, 'w', encoding='utf-8', newline='\n') as f:
            f.write(content)
