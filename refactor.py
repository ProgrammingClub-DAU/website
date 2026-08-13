import subprocess
import re

diff_output = subprocess.check_output(['git', 'diff', 'main...raj/feature-blogs-leaderboard', '--name-status']).decode('utf-8').splitlines()

allowed_packages = ['auth', 'codeforces', 'common', 'config', 'entity', 'security', 'user', 'repository', 'service', 'controller', 'response', 'dto', 'exception']

# Identify renames
for line in diff_output:
    if line.startswith('R'):
        parts = line.split('\t')
        old_file = parts[1]
        new_file = parts[2]
        if 'backend/src/main/java' in old_file:
            subprocess.run(['git', 'mv', old_file, new_file])
            print(f'Moved {old_file} -> {new_file}')
    elif line.startswith('D'):
        parts = line.split('\t')
        old_file = parts[1]
        if 'backend/src/main/java' in old_file:
            subprocess.run(['git', 'rm', '-f', old_file])
            print(f'Deleted {old_file}')

for line in diff_output:
    if line.startswith('A'):
        parts = line.split('\t')
        new_file = parts[1]
        
        # We only want to checkout added files that are NOT in blog or leaderboard or CodeforcesController
        if 'backend/src/main/java' in new_file or 'backend/src/test/java' in new_file:
            if '/blog/' not in new_file and '/leaderboard/' not in new_file and 'CodeforcesController' not in new_file:
                subprocess.run(['git', 'checkout', 'raj/feature-blogs-leaderboard', '--', new_file])
                print(f'Added {new_file}')

