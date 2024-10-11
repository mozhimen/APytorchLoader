# 定义用于计算输入均值的神经网络
import torch
import torch.nn as nn
from PIL import Image
import numpy as np
import torchvision.transforms as transforms


# 定义用于计算输入均值的神经网络
class MeanNet(nn.Module):
    def __init__(self):
        super().__init__()

    def forward(self, x):
        # 计算均值
        mean = x.mean().unsqueeze(0)
        out = torch.cat([mean])
        return mean


if __name__ == "__main__":
    device = torch.device("cpu")

    # 读取图片并转为Tensor类型
    image = Image.open("desk.jpg")
    image = image.resize((256, 256))
    # 对输入图像进行预处理
    trans = transforms.Compose([
        transforms.ToTensor(),
        transforms.Normalize(mean=[0.0, 0.0, 0.0], std=[1.0, 1.0, 1.0])]
    )
    image = trans(image)
    image = image.to(device)

    # 通过mean_net统计图像的信息
    mean_net = MeanNet().to(device)
    mean = mean_net(image)
    print("mean is {:.4}".format(mean.item() * 255))
    # 保存模型
    torch.save(mean_net.state_dict(), "mean_net.pth")
    # 输出结果：
    # mean is 118.9