#include <jni.h>

#include "LC_JNIUtils.h"
#include "libraw.h"

#ifndef LC_AUTO_DEP
#include "javah/com_lightcrafts_image_libs_LibRaw.h"
#endif

using namespace std;
using namespace LightCrafts;

JNIEXPORT void JNICALL Java_com_lightcrafts_image_libs_LibRaw_createLibRawObject(JNIEnv *env, jobject obj) {
    env->SetLongField(obj, env->GetFieldID(env->GetObjectClass(obj), "libRawObject", "J"), (long) new LibRaw());
}

JNIEXPORT void JNICALL Java_com_lightcrafts_image_libs_LibRaw_disposeLibRawObject(JNIEnv *env, jobject obj) {
    jclass libRawClass = env->GetObjectClass(obj);
    jfieldID libRawObjectID = env->GetFieldID(libRawClass, "libRawObject", "J");
    LibRaw *libRaw = (LibRaw *) env->GetLongField(obj, libRawObjectID);
    if (libRaw != NULL) {
        delete libRaw;
        env->SetLongField(obj, libRawObjectID, 0);
    }
}

JNIEXPORT void JNICALL Java_com_lightcrafts_image_libs_LibRaw_recycle(JNIEnv *env, jobject obj) {
    jclass libRawClass = env->GetObjectClass(obj);
    jfieldID libRawObjectID = env->GetFieldID(libRawClass, "libRawObject", "J");
    LibRaw *libRaw = (LibRaw *) env->GetLongField(obj, libRawObjectID);
    if (libRaw != NULL) {
        libRaw->recycle();
    }
}


#define SET_INT(name, value) \
    env->SetIntField(obj, env->GetFieldID(env->GetObjectClass(obj), #name, "I"), libRaw->imgdata.value);

#define SET_LONG(name, value) \
env->SetLongField(obj, env->GetFieldID(env->GetObjectClass(obj), #name, "J"), libRaw->imgdata.value);

#define SET_FLOAT(name, value) \
    env->SetFloatField(obj, env->GetFieldID(env->GetObjectClass(obj), #name, "F"), libRaw->imgdata.value);

#define SET_STRING(name, value) \
    env->SetObjectField(obj, env->GetFieldID(libRawClass, #name, "Ljava/lang/String;"), env->NewStringUTF(libRaw->imgdata.value));

#define SET_ARRAY(name, sig, type, value, size) { \
    jfieldID id = env->GetFieldID(libRawClass, #name, sig); \
    j ## type ## Array jarr = (j ## type ## Array) env->GetObjectField(obj, id); \
    type *arr = (type *) env->GetPrimitiveArrayCritical(jarr, 0); \
    for (int i = 0; i < size; i++) { \
        arr[i] = libRaw->imgdata.value[i]; \
    } \
    env->ReleasePrimitiveArrayCritical(jarr, arr, 0); \
}

#define SET_ARRAY2D(name, sig, type, value, rows, cols) { \
    jfieldID id = env->GetFieldID(libRawClass, #name, sig); \
    jobjectArray jrow = (jobjectArray) env->GetObjectField(obj, id); \
    for (int i = 0; i < rows; i++) { \
        jintArray jarr = (jintArray) env->GetObjectArrayElement(jrow, i); \
        type *arr = (type *) env->GetPrimitiveArrayCritical(jarr, 0); \
        for (int j = 0; j < cols; j++) { \
            arr[j] = libRaw->imgdata.value[i][j]; \
        } \
        env->ReleasePrimitiveArrayCritical(jarr, arr, 0); \
    } \
}

JNIEXPORT jint JNICALL Java_com_lightcrafts_image_libs_LibRaw_openFile(JNIEnv *env, jobject obj, jstring jFileName) {
    jclass libRawClass = env->GetObjectClass(obj);
    LibRaw *libRaw = (LibRaw *) env->GetLongField(obj, env->GetFieldID(libRawClass, "libRawObject", "J"));
    
    if (libRaw == NULL)
        return -1;
    
    // NOTE: if these are not specified here they're not going to be picked up by unpack
    libRaw->imgdata.params.user_flip = 0;
    libRaw->imgdata.params.use_fuji_rotate = 0;
    
    jstring_to_c const cFileName( env, jFileName );
    int status = libRaw->open_file(cFileName);
    
    SET_INT(progress_flags, progress_flags);
    SET_INT(process_warnings, process_warnings);
    
    // libraw_iparams_t
    
    SET_STRING(make, idata.make);
    SET_STRING(model, idata.model);
    
    SET_INT(raw_count, idata.raw_count);
    SET_INT(dng_version, idata.dng_version);
    SET_INT(is_foveon, idata.is_foveon);
    SET_INT(colors, idata.colors);
    SET_INT(filters, idata.filters);
    SET_ARRAY2D(xtrans, "[[I", int, idata.xtrans, 6, 6);
    SET_STRING(cdesc, idata.cdesc);
    
    // libraw_image_sizes_t
    
    SET_INT(raw_height, sizes.raw_height);
    SET_INT(raw_width, sizes.raw_width);
    SET_INT(height, sizes.height);
    SET_INT(width, sizes.width);
    SET_INT(top_margin, sizes.top_margin);
    SET_INT(left_margin, sizes.left_margin);
    SET_INT(iheight, sizes.iheight);
    SET_INT(iwidth, sizes.iwidth);
    SET_INT(raw_pitch, sizes.raw_pitch);
    SET_FLOAT(pixel_aspect, sizes.pixel_aspect);
    SET_INT(flip, sizes.flip);
    SET_ARRAY2D(mask, "[[I", int, sizes.mask, 8, 4);
    
    // libraw_colordata_t
    
    // TODO: ushort       curve[0x10000];
    
    SET_ARRAY(cblack, "[I", int, color.cblack, 8);
    SET_INT(black, color.black);
    SET_INT(data_maximum, color.data_maximum);
    SET_INT(maximum, color.maximum);
    SET_ARRAY2D(white, "[[I", int, color.white, 8, 8);
    
    SET_ARRAY(cam_mul, "[F", float, color.cam_mul, 4);
    SET_ARRAY(pre_mul, "[F", float, color.pre_mul, 4);
    
    SET_ARRAY2D(cmatrix, "[[F", float, color.cmatrix, 3, 4);
    SET_ARRAY2D(rgb_cam, "[[F", float, color.rgb_cam, 3, 4);
    SET_ARRAY2D(cam_xyz, "[[F", float, color.cam_xyz, 4, 3);

    // TODO: ph1_t       phase_one_data
    
    SET_FLOAT(flash_used, color.flash_used);
    SET_FLOAT(canon_ev, color.canon_ev);
    SET_STRING(model2, color.model2);
    
    // TODO: void        *profile
    
    SET_INT(profile_length, color.profile_length);
    SET_ARRAY(black_stat, "[I", int, color.black_stat, 8);
    
    SET_FLOAT(iso_speed, other.iso_speed);
    SET_FLOAT(shutter, other.shutter);
    SET_FLOAT(aperture, other.aperture);
    SET_FLOAT(focal_len, other.focal_len);
    SET_LONG(timestamp, other.timestamp);
    SET_INT(shot_order, other.shot_order);
    SET_ARRAY(gpsdata, "[I", int, other.gpsdata, 32);
    SET_STRING(desc, other.desc);
    SET_STRING(artist, other.artist);
    
    // libraw_thumbnail_t
    
    SET_INT(tformat, thumbnail.tformat);
    SET_INT(twidth, thumbnail.twidth);
    SET_INT(theight, thumbnail.theight);
    SET_INT(tlength, thumbnail.tlength);
    SET_INT(tcolors, thumbnail.tcolors);
    
    // TODO: char       *thumb
    
    if (libRaw->imgdata.idata.filters) {
        char filter_pattern[17];
        if (!libRaw->imgdata.idata.cdesc[3]) libRaw->imgdata.idata.cdesc[3] = 'G';
        for (int i=0; i < 16; i++)
            filter_pattern[i] = libRaw->imgdata.idata.cdesc[libRaw->fcol(i >> 1,i & 1)];
        filter_pattern[10] = NULL;
        env->SetObjectField(obj, env->GetFieldID(libRawClass, "filter_pattern", "Ljava/lang/String;"), env->NewStringUTF(filter_pattern));
	}
    
    return status;
}

struct callback {
    JNIEnv *env;
    jobject obj;
    jmethodID mid;
};

extern "C" int my_progress_callback(void *data, enum LibRaw_progress p, int iteration, int expected)
{
    char *passed_string = (char *) data;
    clock_t time = clock();
    // fprintf(stderr, "Callback: %s  pass %d of %d, data passed: %p, time: %f\n", libraw_strprogress(p), iteration, expected, passed_string, time / (float) CLOCKS_PER_SEC);

    if (data != NULL) {
        callback *c = (callback *) data;
        return c->env->CallIntMethod(c->obj, c->mid, p, iteration, expected);
    } else
        return 0;
}

JNIEXPORT jshortArray JNICALL Java_com_lightcrafts_image_libs_LibRaw_unpackImage(JNIEnv *env, jobject obj, jboolean interpolate, jboolean half_size) {
    jclass libRawClass = env->GetObjectClass(obj);
    LibRaw *libRaw = (LibRaw *) env->GetLongField(obj, env->GetFieldID(libRawClass, "libRawObject", "J"));
    
    if (libRaw == NULL)
        return NULL;
    
    int status = libRaw->unpack();
    if (status != 0)
        return NULL;
    
    // NOTE: see note in openFile
    libRaw->imgdata.params.user_flip = 0;
    libRaw->imgdata.params.use_fuji_rotate = 0;
    libRaw->imgdata.params.gamm[0] = libRaw->imgdata.params.gamm[1] = 1;
    if (half_size)
        libRaw->imgdata.params.half_size = 1;
    // else if (!interpolate)
    //     libRaw->imgdata.params.document_mode = 1;
    else
        libRaw->imgdata.params.four_color_rgb = 1;
    libRaw->imgdata.params.highlight = 1;
    libRaw->imgdata.params.output_bps = 16;
    libRaw->imgdata.params.output_color = 0; // RAW
    // libRaw->imgdata.params.output_tiff = 1;
    
    jclass cls = env->GetObjectClass(obj);
    jmethodID mid = env->GetMethodID(cls, "progress", "(III)I");
    callback data = {env, obj, mid};
    if (mid != NULL) {
        libRaw->set_progress_handler(my_progress_callback, (void *) &data);
    }
    
    status = libRaw->dcraw_process();
    
    if (status == 0) {
        /* char *outfn = "/Stuff/rawout.tiff";
        printf("Dumping raw file to %s\n", outfn);
        if( LIBRAW_SUCCESS != (status = libRaw->dcraw_ppm_tiff_writer(outfn)))
            fprintf(stderr,"Cannot write %s: %s\n",outfn,libraw_strerror(status)); */
        
        int bands = interpolate ? 3 : 1;
        
        libraw_processed_image_t *image = libRaw->dcraw_make_mem_image(&status);
        if (image != NULL) {
            int buffer_size = bands * image->width * image->height;
            jshortArray jimage_data = env->NewShortArray((jsize) buffer_size);
            env->SetShortArrayRegion(jimage_data, 0, bands * image->width * image->height, (jshort *) image->data);
            free(image);
            return jimage_data;
        }
    }
    return NULL;
}

JNIEXPORT jbyteArray JNICALL Java_com_lightcrafts_image_libs_LibRaw_unpackThumb(JNIEnv *env, jobject obj) {
    jclass libRawClass = env->GetObjectClass(obj);
    LibRaw *libRaw = (LibRaw *) env->GetLongField(obj, env->GetFieldID(libRawClass, "libRawObject", "J"));

    if (libRaw == NULL)
        return NULL;
    
    int status = libRaw->unpack_thumb();
    
    if (status == 0) {
        libraw_processed_image_t *thumb = libRaw->dcraw_make_mem_thumb(&status);
        if (thumb != NULL) {
            jbyteArray jthumbnail_data = env->NewByteArray((jsize) thumb->data_size);
            env->SetByteArrayRegion(jthumbnail_data, 0, thumb->data_size, (jbyte *) thumb->data);
            SET_INT(tformat, thumbnail.tformat);
            free(thumb);
            return jthumbnail_data;
        }
    }
    return NULL;
}

