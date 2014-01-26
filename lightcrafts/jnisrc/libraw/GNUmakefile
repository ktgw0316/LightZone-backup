HIGH_PERFORMANCE:=	1
USE_ICC_HERE:=		1
USE_X64_HERE:=		1

TARGET_BASE:=		LIBRAW

# Uncomment to compile in debug mode.
#DEBUG:=			true

##
# I didn't want to figure out how to tweak this LibRaw stuff to have it conform
# to the way JNI source is built, so I'll just define the TARGET rule manually.
##
JNI_MANUAL_TARGET:=	1

LIBRAW_PATH:=		LibRaw/LibRaw-0.16.0
JNI_EXTRA_INCLUDES:=	-I$(LIBRAW_PATH) -I$(LIBRAW_PATH)/libraw

JAVAH_CLASSES:=		com.lightcrafts.image.libs.LibRaw

ROOT:=			../../..
include			../jni.mk

LIBRAW_INCLUDES:=	$(wildcard $(LIBRAW_PATH)/internal/*.h) $(wildcard $(LIBRAW_PATH)/libraw/*.h)
LIBRAW_SOURCE:=		$(LIBRAW_PATH)/internal/dcraw_common.cpp $(LIBRAW_PATH)/internal/dcraw_fileio.cpp \
			$(LIBRAW_PATH)/internal/demosaic_packs.cpp \
			$(LIBRAW_PATH)/src/libraw_cxx.cpp $(LIBRAW_PATH)/src/libraw_c_api.cpp \
			$(LIBRAW_PATH)/src/libraw_datastream.cpp \
			LibRawJNI.cpp # demosaic.cpp
LIBRAW_LDFLAGS:=	$(filter-out $(PLATFORM_LDFLAGS),$(LDFLAGS))
LIBRAW_CFLAGS:=		-DLIBRAW_NOTHREADS

ifeq ($(PLATFORM),Windows)
  LIBRAW_CFLAGS+=	-DLIBRAW_NODLL
endif

ifeq ($(USE_ICC),1)
  LIBRAW_CFLAGS+=       -ipo
else
  LIBRAW_CFLAGS+=	-w
endif

$(TARGET): $(LIBRAW_SOURCE) $(LIBRAW_INCLUDES)
	-$(MKDIR) $(TARGET_DIR)
	$(CC_LINK) $(CFLAGS) $(LIBRAW_CFLAGS) $(INCLUDES) $(LIBRAW_LDFLAGS) -o $@ $(LIBRAW_SOURCE)
	$(RM) $@.dSYM

# vim:set noet sw=8 ts=8:
