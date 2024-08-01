#MAGISK
############################################
# Magisk Uninstaller (updater-script)
############################################


# Pure bash dirname implementation
getdir() {
  case "$1" in
    */*)
      dir=${1%/*}
      if [ -z $dir ]; then
        echo "/"
      else
        echo $dir
      fi
    ;;
    *) echo "." ;;
  esac
}


############
# Uninstall
############

if [ -z $SOURCEDMODE ]; then
  # Switch to the location of the script file
  cd "$(getdir "${BASH_SOURCE:-$0}")"
  # Load utility functions
  . ./util_functions.sh
  # Check if 64-bit
  api_level_arch_detect
fi

BOOTIMAGE="$1"

[ -e "$BOOTIMAGE" ] || abort "$BOOTIMAGE does not exist!"

CHROMEOS=false

ui_print "- Unpacking boot image"
# Dump image for MTD/NAND character device boot partitions
if [ -c $BOOTIMAGE ]; then
  nanddump -f boot.img $BOOTIMAGE
  BOOTNAND=$BOOTIMAGE
  BOOTIMAGE=boot.img
fi
./magiskboot unpack "$BOOTIMAGE"

case $? in
  0 ) ;;
  1 )
    abort "! Unsupported/Unknown image format"
    ;;
  2 )
    ui_print "- ChromeOS boot image detected"
    CHROMEOS=true
    ;;
  * )
    abort "! Unable to unpack boot image"
    ;;
esac

# Restore the original boot partition path
[ "$BOOTNAND" ] && BOOTIMAGE=$BOOTNAND

# Detect boot image state
ui_print "- Checking ramdisk status"
if [ -e ramdisk.cpio ]; then
  ./magiskboot cpio ramdisk.cpio test
  STATUS=$?
else
  # Stock A only system-as-root
  STATUS=0
fi
case $((STATUS & 3)) in
  0 )  # Stock boot
    ui_print "- Stock boot image detected"
    ;;
  1 )  # Magisk patched
    ui_print "- Magisk patched image detected"
    # Find SHA1 of stock boot image
    ./magiskboot cpio ramdisk.cpio "extract .backup/.magisk config.orig"
    if [ -f config.orig ]; then
      chmod 0644 config.orig
      SHA1=$(grep_prop SHA1 config.orig)
      rm config.orig
    fi
    BACKUPDIR=/data/magisk_backup_$SHA1
    if [ -d $BACKUPDIR ]; then
      ui_print "- Restoring stock boot image"
      gzip -dk $BACKUPDIR/boot.img.gz
      mv $BACKUPDIR/boot.img new-boot.img
      #flash_image $BACKUPDIR/boot.img.gz $FLASHIMAGE
    else
      ui_print "! Boot image backup unavailable"
      ui_print "- Restoring ramdisk with internal backup"
      ./magiskboot cpio ramdisk.cpio restore
      if ! ./magiskboot cpio ramdisk.cpio "exists init"; then
        # A only system-as-root
        rm -f ramdisk.cpio
      fi
      ./magiskboot repack $BOOTIMAGE
      # Sign chromeos boot
      $CHROMEOS && sign_chromeos
      ui_print "- Flashing restored boot image"
      #flash_image new-boot.img $FLASHIMAGE || abort "! Insufficient partition size"
    fi
    ;;
  2 )  # Unsupported
    ui_print "! Boot image patched by unsupported programs"
    abort "! Cannot uninstall"
    ;;
esac