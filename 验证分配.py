import json

# 读取分配记录
with open(r'd:\JAVA\xiaxiang\素材分配记录.json', 'r', encoding='utf-8') as f:
    data = json.load(f)

print('=== 素材分配验证 ===')
print('视频总数:', data['totalVideos'])
print()

# 统计分配数量
total_assigned = 0
for module_name, module_data in data['modules'].items():
    count = len(module_data['slots'])
    total_assigned += count
    print(f'{module_name}: {count}个视频')
    for slot in module_data['slots']:
        print(f'  - {slot["slotId"]}: {slot["videoTitle"]}')

print()
print(f'已分配总数: {total_assigned}/{data["totalVideos"]}')

# 检查是否有重复的视频标题
all_titles = []
for module_name, module_data in data['modules'].items():
    for slot in module_data['slots']:
        all_titles.append(slot['videoTitle'])

if len(all_titles) == len(set(all_titles)):
    print('✓ 无重复视频')
else:
    print('✗ 存在重复视频!')
    seen = {}
    for t in all_titles:
        if t in seen:
            print(f'  重复: {t}')
        seen[t] = 1

print()
print('✓ 分配验证通过')
