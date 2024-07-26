#!/usr/bin/python
# coding: utf-8
import requests
import os
import sys
import oss2
from oss2.exceptions import NoSuchKey, OssError, NotFound
import time

api_url = "https://api.github.com/repos/bmax121/KernelPatch/releases/latest"
download_url = "https://mirror.ghproxy.com/https://github.com/bmax121/KernelPatch/releases/download/"

# 阿里云OSS 配置
access_id = "************************"
access_key = "************************"
bucket_name = "kpatch"
endpoint = "https://oss-cn-shenzhen.aliyuncs.com"

# 测试号 配置
weixin_appID = "wxe3e5f9013782a443"
weixin_appsecret = "afd1d76471981ed812aa051281cd6ac1"
weixin_user_id = "oWCnq6Jf5-hdozP0aN-C5dQzigUU"
weixin_template_id = "ULyapScH_7ubuQHBLSyoLWMesG_s-lalUdocqpObXiA"

weixin_push_data = dict()

if sys.version_info[0] == 2:
    reload(sys)
    sys.setdefaultencoding('utf8')

try:
    auth = oss2.Auth(access_id, access_key)
    bucket = oss2.Bucket(auth, endpoint, bucket_name)
except OssError:
    print("无法连接到阿里云OSS服务器，请检查[AccessKeyId/AccessKeySecret/Endpoint]设置是否正确!")
except OsError:
    print("无法连接到阿里云OSS服务器，请检查[AccessKeyId/AccessKeySecret/Endpoint]设置是否正确!") 
except Exception as e:
    print("无法连接到阿里云OSS服务器" + str(e))

if not bucket:
    sys.exit(1)

def download_file(tag, fliename):
    print("download_file: " + fliename)
    try:
        r = requests.get(download_url + tag + "/" + fliename)
        if r.status_code == 200:
            return r.content
        else:
            return False
    except Exception as e:
        print("download_file fail: " + str(e))
        return False


def check_file(tag, fliename):
    try:
        file_info = bucket.get_object(tag + "/" + fliename).read()
        return True
    except Exception as e:
        return False

def upload_file(path, local_path):
    print("upload_file: " + path)
    if len(local_path) < 100:
        return False
    result = bucket.put_object(path, local_path)
    if result.status == 200:
        return True

r = requests.get(api_url)
all_info = r.json()
if not all_info:
    print("request Api fail, please try again")
    sys.exit(1)
last_tag = all_info['tag_name']

if not last_tag:
    print("get tag_name fail")
    sys.exit(1)

print("Check Tag：" + last_tag)
weixin_push_data["tag"] = {"value": last_tag}
weixin_push_data["time"] = {"value": time.strftime("%Y-%m-%d %H:%M:%S", time.localtime())}

upload_file("latest.json", r.content)


if not check_file(last_tag, "kpimg"):
    kpimg_data = download_file(last_tag, "kpimg-android")
    if kpimg_data:
        if upload_file(last_tag + "/kpimg", kpimg_data):
            weixin_push_data["kpimg"] = {"value": "success"}
        else:
            weixin_push_data["kpimg"] = {"value": "fail"}
    
if not check_file(last_tag, "kptools"):
    kptools_data = download_file(last_tag, "kptools-android")
    if kptools_data:
        if upload_file(last_tag + "/kptools", kptools_data):
            weixin_push_data["kptools"] = {"value": "success"}
        else:
            weixin_push_data["kptools"] = {"value": "fail"}
        
if not check_file(last_tag, "kpatch"):
    kpatch_data = download_file(last_tag, "kpatch-android")
    if kpatch_data:
        if upload_file(last_tag + "/kpatch", kpatch_data):
            weixin_push_data["kpatch"] = {"value": "success"}
        else:
            weixin_push_data["kpatch"] = {"value": "fail"}

if weixin_push_data.get("kpatch"):
    post_url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid={}&secret={}".format(weixin_appID, weixin_appsecret)
    access_token = requests.get(post_url).json()['access_token']
    url = "https://api.weixin.qq.com/cgi-bin/message/template/send?access_token={}".format(access_token)
    data = {
        "touser": weixin_user_id,
        "template_id": weixin_template_id,
        "url": "http://weixin.qq.com/download",
        "topcolor": "#FF0000",
        "data": weixin_push_data
    }
    headers = {
        'Content-Type': 'application/json',
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36'
    }
    requests.post(url, headers=headers, json=data)

print("Done")