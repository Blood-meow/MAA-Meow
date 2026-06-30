package com.aliothmoon.maameow.maa;

import android.app.UiAutomation;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.KeyCharacterMap;
import android.view.MotionEvent;

import java.lang.reflect.Method;

import com.aliothmoon.maameow.ITouchEventCallback;
import com.aliothmoon.maameow.third.Ln;
import com.aliothmoon.maameow.third.wrappers.InputManager;
import com.aliothmoon.maameow.third.wrappers.ServiceManager;


public final class InputControlUtils {

    private static final String TAG = "InputControlUtils";

    private static InputManager manager;
    private static volatile ITouchEventCallback touchCallback;

    private static InputManager getManager() {
        if (manager == null) {
            manager = ServiceManager.getInputManager();
        }
        return manager;
    }

    private static final int DEFAULT_DEVICE_ID = 0;
    private static final int DEFAULT_SOURCE = InputDevice.SOURCE_TOUCHSCREEN;

    private static final MotionEvent.PointerProperties[] POINTER_PROPERTIES = new MotionEvent.PointerProperties[1];
    private static final MotionEvent.PointerCoords[] POINTER_COORDS = new MotionEvent.PointerCoords[1];

    static {
        MotionEvent.PointerProperties props = new MotionEvent.PointerProperties();
        props.id = 0;
        props.toolType = MotionEvent.TOOL_TYPE_FINGER;
        POINTER_PROPERTIES[0] = props;

        POINTER_COORDS[0] = new MotionEvent.PointerCoords();
    }

    private static long currentDownTime = 0;

    private InputControlUtils() {
    }

    private static void setPointerCoords(float x, float y, float pressure) {
        MotionEvent.PointerCoords coords = POINTER_COORDS[0];
        coords.x = Math.max(0, x);
        coords.y = Math.max(0, y);
        coords.pressure = pressure;
        coords.size = 1.0f;
    }

    private static MotionEvent obtainTouchEvent(long downTime, long eventTime, int action,
                                                float x, float y, float pressure) {
        setPointerCoords(x, y, pressure);
        return MotionEvent.obtain(
                downTime, eventTime, action,
                1, POINTER_PROPERTIES, POINTER_COORDS,
                0, 0,
                1.0f, 1.0f,
                DEFAULT_DEVICE_ID, 0, DEFAULT_SOURCE, 0
        );
    }

    private static boolean injectInputEvent(MotionEvent event, int displayId, int mode) {
        try {
            if (!setDisplayId(event, displayId)) {
                return false;
            }
            notifyTouchCallback(event);
            return getManager().injectInputEvent(event, mode);
        } finally {
            event.recycle();
        }
    }

    public static void setTouchCallback(ITouchEventCallback callback) {
        touchCallback = callback;
    }

    private static void notifyTouchCallback(MotionEvent event) {
        ITouchEventCallback callback = touchCallback;
        if (callback == null) {
            return;
        }
        try {
            callback.onCallback(Math.round(event.getX()), Math.round(event.getY()), event.getActionMasked());
        } catch (RemoteException | RuntimeException e) {
            touchCallback = null;
            Ln.w(TAG + ": touch callback failed, clearing registration", e);
        }
    }


    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean setDisplayId(InputEvent event, int displayId) {
        return displayId == 0 || InputManager.setDisplayId(event, displayId);
    }

    public static synchronized boolean down(int x, int y, int displayId) {
        // 在按下前，如果发现上一次序列未正常结束，强制发送一个 ACTION_CANCEL 确保触控槽位被清空
        if (currentDownTime != 0) {
            MotionEvent cancelEvent = obtainTouchEvent(currentDownTime, SystemClock.uptimeMillis(),
                    MotionEvent.ACTION_CANCEL, (float) x, (float) y, 0.0f);
            injectInputEvent(cancelEvent, displayId, InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
        }

        currentDownTime = SystemClock.uptimeMillis();

        MotionEvent motionEvent = obtainTouchEvent(currentDownTime, currentDownTime,
                MotionEvent.ACTION_DOWN, (float) x, (float) y, 1.0f);

        // DOWN 事件必须使用 WAIT_FOR_FINISH 模式，确保起始状态被系统成功接收
        return injectInputEvent(motionEvent, displayId, InputManager.INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH);
    }

    public static synchronized boolean move(int x, int y, int displayId) {
        if (currentDownTime == 0) return false;

        long eventTime = SystemClock.uptimeMillis();
        MotionEvent motionEvent = obtainTouchEvent(currentDownTime, eventTime,
                MotionEvent.ACTION_MOVE, (float) x, (float) y, 1.0f);
        return injectInputEvent(motionEvent, displayId, InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
    }

    public static synchronized boolean up(int x, int y, int displayId) {
        if (currentDownTime == 0) return false;

        long eventTime = SystemClock.uptimeMillis();
        MotionEvent motionEvent = obtainTouchEvent(currentDownTime, eventTime,
                MotionEvent.ACTION_UP, (float) x, (float) y, 0.0f);
        
        boolean result = injectInputEvent(motionEvent, displayId, InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
        
        // 抬起后重置时间戳
        currentDownTime = 0;
        return result;
    }

    public static boolean keyDown(int keyCode, int displayId) {
        long downTime = SystemClock.uptimeMillis();
        KeyEvent keyEvent = new KeyEvent(downTime, downTime, KeyEvent.ACTION_DOWN, keyCode, 0);

        if (!setDisplayId(keyEvent, displayId)) {
            return false;
        }
        return getManager().injectInputEvent(keyEvent, InputManager.INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH);
    }

    public static boolean keyUp(int keyCode, int displayId) {
        long upTime = SystemClock.uptimeMillis();
        KeyEvent keyEvent = new KeyEvent(upTime, upTime, KeyEvent.ACTION_UP, keyCode, 0);

        if (!setDisplayId(keyEvent, displayId)) {
            return false;
        }

        return getManager().injectInputEvent(keyEvent, InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
    }

    public static boolean keyDownWithMeta(int keyCode, int metaState, int displayId) {
        long downTime = SystemClock.uptimeMillis();
        KeyEvent keyEvent = new KeyEvent(downTime, downTime, KeyEvent.ACTION_DOWN, keyCode, 0, metaState);
        if (!setDisplayId(keyEvent, displayId)) {
            return false;
        }
        return getManager().injectInputEvent(keyEvent, InputManager.INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH);
    }

    public static boolean keyUpWithMeta(int keyCode, int metaState, int displayId) {
        long upTime = SystemClock.uptimeMillis();
        KeyEvent keyEvent = new KeyEvent(upTime, upTime, KeyEvent.ACTION_UP, keyCode, 0, metaState);
        if (!setDisplayId(keyEvent, displayId)) {
            return false;
        }
        return getManager().injectInputEvent(keyEvent, InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
    }

    public static boolean injectEvent(KeyEvent keyEvent, int displayId) {
        if (!setDisplayId(keyEvent, displayId)) {
            return false;
        }
        return getManager().injectInputEvent(keyEvent, InputManager.INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH);
    }

    @SuppressWarnings("deprecation")
    public static boolean sendText(String text, int displayId) {
        if (text == null || text.isEmpty()) return true;
        try {
            // Get clipboard binder via reflection (hidden API)
            Method getServiceMethod = Class.forName("android.os.ServiceManager")
                    .getDeclaredMethod("getService", String.class);
            IBinder binder = (IBinder) getServiceMethod.invoke(null, "clipboard");
            if (binder == null) {
                Ln.e("InputControlUtils: sendText - clipboard binder null");
                return false;
            }
            // IClipboard.Stub.asInterface(binder)
            Class<?> stubClass = Class.forName("android.content.IClipboard$Stub");
            Method asInterface = stubClass.getMethod("asInterface", IBinder.class);
            Object clipboard = asInterface.invoke(null, binder);

            // Save original clip
            Method getPrimaryClip = clipboard.getClass().getMethod("getPrimaryClip", String.class, String.class, int.class);
            Object savedClip = getPrimaryClip.invoke(clipboard, "com.android.shell", null, 0);

            // Set new clip with our text
            Class<?> clipDataClass = Class.forName("android.content.ClipData");
            Method newPlainText = clipDataClass.getMethod("newPlainText", CharSequence.class, CharSequence.class);
            Object newClip = newPlainText.invoke(null, "input", text);
            Method setPrimaryClip = clipboard.getClass().getMethod("setPrimaryClip", clipDataClass, String.class, String.class, int.class);
            setPrimaryClip.invoke(clipboard, newClip, "com.android.shell", null, 0);

            // Inject Ctrl+V on virtual display
            long downTime = SystemClock.uptimeMillis();
            int metaCtrl = KeyEvent.META_CTRL_ON | KeyEvent.META_CTRL_LEFT_ON;
            KeyEvent vDown = new KeyEvent(downTime, downTime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_V, 0, metaCtrl);
            if (!setDisplayId(vDown, displayId)) return false;
            getManager().injectInputEvent(vDown, InputManager.INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH);
            KeyEvent vUp = new KeyEvent(downTime, SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, KeyEvent.KEYCODE_V, 0, metaCtrl);
            if (!setDisplayId(vUp, displayId)) return false;
            getManager().injectInputEvent(vUp, InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);

            // Restore original clip
            if (savedClip != null) {
                setPrimaryClip.invoke(clipboard, savedClip, "com.android.shell", null, 0);
            }
            return true;
        } catch (Exception e) {
            Ln.e("InputControlUtils: sendText error: " + e.getMessage());
            return false;
        }
    }

}
