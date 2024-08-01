#!/system/bin/sh

#From Magisk
flash_image() {
  local CMD1
  case "$1" in
    *.gz) CMD1="gzip -d < '$1' 2>/dev/null";;
    *)    CMD1="cat '$1'";;
  esac
  if [ -b "$2" ]; then
    local img_sz=$(stat -c '%s' "$1")
    local blk_sz=$(blockdev --getsize64 "$2")
    [ "$img_sz" -gt "$blk_sz" ] && return 1
    blockdev --setrw "$2"
    local blk_ro=$(blockdev --getro "$2")
    [ "$blk_ro" -eq 1 ] && return 2
    eval "$CMD1" | cat - /dev/zero > "$2" 2>/dev/null
  elif [ -c "$2" ]; then
    flash_eraseall "$2" >&2
    eval "$CMD1" | nandwrite -p "$2" - >&2
  else
    ui_print "- Not block or char device, storing image"
    eval "$CMD1" > "$2" 2>/dev/null
  fi
  return 0
}