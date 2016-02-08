LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional
LOCAL_PACKAGE_NAME := GateworksDemo
LOCAL_CERTIFICATE := platform

LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_STATIC_JAVA_LIBRARIES := GateworksUtil
LOCAL_LDLIBS += -llog

include $(BUILD_PACKAGE)
include $(call all-makefiles-under,$(LOCAL_PATH))
