from PIL import Image
import numpy as np

src = Image.open(r'F:\Android\ReadPlusNext\.tmp\icon-src\playstore.png').convert('RGB')
a = np.asarray(src).astype(np.float32)
luma = 0.299*a[:, :, 0] + 0.587*a[:, :, 1] + 0.114*a[:, :, 2]
blue_luma = 0.299*19 + 0.587*117 + 0.114*193
alpha = np.clip((luma - blue_luma) / (255.0 - blue_luma), 0.0, 1.0)
mask = alpha > 0.06
ys, xs = np.where(mask)
print('bbox x', xs.min(), xs.max(), 'y', ys.min(), ys.max(), 'w', xs.max()-xs.min()+1, 'h', ys.max()-ys.min()+1)

ic = Image.open(r'F:\Android\ReadPlusNext\.tmp\icon-src\icon.png').convert('RGB')
b = np.asarray(ic)
print('icon size', ic.size, 'corner', b[2, 2], 'center', b[256, 256])
cols, counts = np.unique(b.reshape(-1, 3), axis=0, return_counts=True)
for i in np.argsort(-counts)[:4]:
    print('icon color', cols[i], counts[i])
