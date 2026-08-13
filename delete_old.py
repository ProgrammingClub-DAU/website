import subprocess

diff_output = subprocess.check_output(['git', 'diff', 'main...raj/feature-blogs-leaderboard', '--name-status']).decode('utf-8').splitlines()

for line in diff_output:
    if line.startswith('D'):
        parts = line.split('\t')
        old_file = parts[1]
        if 'backend/' in old_file:
            subprocess.run(['git', 'rm', '-f', old_file])
            print(f'Deleted old file: {old_file}')
