#!/system/bin/sh
#######################################################################################
# APatch Boot Image Unpatcher
#######################################################################################

flash_image() {
  local CMD1
  case "$1" in
    *.gz) CMD1="gzip -d < '$1' 2>/dev/null";;
    *)    CMD1="cat '$1'";;
  esac
  if [ -b "$2" ]; then {
      local img_sz=$(stat -c '%s' "$1")
      local blk_sz=$(blockdev --getsize64 "$2")
      local blk_bs=$(blockdev --getbsz "$2")
      [ "$img_sz" -gt "$blk_sz" ] && return 1
      blockdev --setrw "$2"
      local blk_ro=$(blockdev --getro "$2")
      [ "$blk_ro" -eq 1 ] && return 2
      eval "$CMD1" | dd of="$2" bs="$blk_bs" iflag=fullblock conv=notrunc,fsync 2>/dev/null
      sync
  } elif [ -c "$2" ]; then {
      flash_eraseall "$2" >&2
      eval "$CMD1" | nandwrite -p "$2" - >&2
  } else {
      echo "- Not block or char device, storing image"
      eval "$CMD1" > "$2" 2>/dev/null
  } fi
  return 0
}


ARCH=$(getprop ro.product.cpu.abi)

echo "****************************"
echo " APatch Boot Image Unpatcher"
echo "****************************"

BOOTIMAGE=$1

[ -e "$BOOTIMAGE" ] || { echo "- $BOOTIMAGE does not exist!"; exit 1; }

echo "- Target image: $BOOTIMAGE"

  # Check for dependencies
command -v ./magiskboot >/dev/null 2>&1 || { echo "- Command magiskboot not found!"; exit 1; }
command -v ./kptools >/dev/null 2>&1 || { echo "- Command kptools not found!"; exit 1; }

if [ ! -f kernel ]; then
echo "- Unpacking boot image"
./magiskboot unpack "$BOOTIMAGE" >/dev/null 2>&1
if [ $? -ne 0 ]; then
    >&2 echo "- Unpack error: $?"
    exit $?
  fi
fi

mv kernel kernel.ori

echo "- Unpatching kernel"
./kptools -u --image kernel.ori --out kernel

if [ $? -ne 0 ]; then
  >&2 echo "- Unpatch error: $?"
  exit $?
fi

echo "- Repacking boot image"
./magiskboot repack "$BOOTIMAGE" >/dev/null 2>&1

if [ $? -ne 0 ]; then
  >&2 echo "- Repack error: $?"
  exit $?
fi

if [ -f "new-boot.img" ]; then
  echo "- Flashing boot image"
  flash_image new-boot.img "$BOOTIMAGE"

  if [ $? -ne 0 ]; then
    >&2 echo "- Flash error: $?"
    exit $?
  fi
fi

echo "- Flash successful"

# Reset any error code
true
