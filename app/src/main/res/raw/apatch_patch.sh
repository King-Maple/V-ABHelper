#!/system/bin/sh
#######################################################################################
# APatch Boot Image Patcher
#######################################################################################
#
# Usage: boot_patch.sh <superkey> <bootimage> [ARGS_PASS_TO_KPTOOLS]
#
# This script should be placed in a directory with the following files:
#
# File name          Type          Description
#
# boot_patch.sh      script        A script to patch boot image for APatch.
#                  (this file)      The script will use files in its same
#                                  directory to complete the patching process.
# bootimg            binary        The target boot image
# kpimg              binary        KernelPatch core Image
# kptools            executable    The KernelPatch tools binary to inject kpimg to kernel Image
# magiskboot         executable    Magisk tool to unpack boot.img.
#
#######################################################################################

ARCH=$(getprop ro.product.cpu.abi)

echo "****************************"
echo " APatch Boot Image Patcher"
echo "****************************"

SUPERKEY=$1
BOOTIMAGE=$2
shift 2

[ -z "$SUPERKEY" ] && { abort "- SuperKey empty!"; }
[ -e "$BOOTIMAGE" ] || { abort "- $BOOTIMAGE does not exist!"; }

# Check for dependencies
command -v ./magiskboot >/dev/null 2>&1 || { abort "- Command magiskboot not found!"; }
command -v ./kptools >/dev/null 2>&1 || { abort "- Command kptools not found!"; }

if [ ! -f kernel ]; then
echo "- Unpacking boot image"
./magiskboot unpack "$BOOTIMAGE" >/dev/null 2>&1
  if [ $? -ne 0 ]; then
    abort "- Unpack error: $?"
  fi
fi

mv kernel kernel.ori

echo "- Patching kernel"

./kptools -p -i kernel.ori -s "$SUPERKEY" -k kpimg -o kernel "$@"

if [ $? -ne 0 ]; then
  abort "- Patch kernel error: $?"
fi

echo "- Repacking boot image"
./magiskboot repack "$BOOTIMAGE" >/dev/null 2>&1

if [ $? -ne 0 ]; then
  abort "- Repack error: $?"
fi

echo "- Cleaning up"
./magiskboot cleanup >/dev/null 2>&1
rm -f kernel.ori

echo "- Patching kernel Success"