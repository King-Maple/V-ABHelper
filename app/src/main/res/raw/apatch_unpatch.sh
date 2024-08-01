#!/system/bin/sh
#######################################################################################
# APatch Boot Image Unpatcher
#######################################################################################

ARCH=$(getprop ro.product.cpu.abi)

echo "****************************"
echo " APatch Boot Image Unpatcher"
echo "****************************"

BOOTIMAGE=$1

[ -e "$BOOTIMAGE" ] || { abort "- $BOOTIMAGE does not exist!"; }

echo "- Target image: $BOOTIMAGE"

  # Check for dependencies
command -v ./magiskboot >/dev/null 2>&1 || { abort "- Command magiskboot not found!"; }
command -v ./kptools >/dev/null 2>&1 || { abort "- Command kptools not found!"; }

if [ ! -f kernel ]; then
echo "- Unpacking boot image"
./magiskboot unpack "$BOOTIMAGE" >/dev/null 2>&1
if [ $? -ne 0 ]; then
    abort "- Unpatch error: $?"
  fi
fi

mv kernel kernel.ori

echo "- Unpatching kernel"
./kptools -u --image kernel.ori --out kernel

if [ $? -ne 0 ]; then
  abort "- Unpatch error: $?"
fi

echo "- Repacking boot image"
./magiskboot repack "$BOOTIMAGE" >/dev/null 2>&1

if [ $? -ne 0 ]; then
  abort "- Repack error: $?"
fi

echo "- Cleaning up"
./magiskboot cleanup >/dev/null 2>&1
rm -f kernel.ori

echo "- Repacking success"
