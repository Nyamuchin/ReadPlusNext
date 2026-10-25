from PIL import Image
import numpy as np

src = Image.open(r'F:\Android\ReadPlusNext\.tmp\icon-src\playstore.png').convert('RGB')
a = np.asarray(src).astype(np.float32)
print('size', src.size)
print('corner', a[2,2], 'center', a[256,256])
# unique colors count
flat = a.reshape(-1,3).astype(np.uint8)
cols, counts = np.unique(flat, axis=0, return_counts=True)
order = np.argsort(-counts)
for i in order[:5]:
    print('color', cols[i], 'count', counts[i])
