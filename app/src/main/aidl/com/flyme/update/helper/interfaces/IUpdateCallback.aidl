// IUpdateCallback.aidl
package com.flyme.update.helper.interfaces;

import com.flyme.update.helper.interfaces.IUpdateCallback;

interface IUpdateCallback {
      void onPayloadApplicationComplete(int errorCode);

      void onStatusUpdate(int status, float percent);
}