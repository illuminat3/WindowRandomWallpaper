import ctypes
import sys

def set_wallpaper(image_path):
    ctypes.windll.user32.SystemParametersInfoW(20, 0, image_path, 3)

if __name__ == "__main__":
    image_path = sys.argv[1]
    set_wallpaper(image_path)