Workflow integration: Aperture
==============================

Aperture (from Apple) allows you to select another application as an
"external editor" for editing your photos. You can configure LightZone
to be such an editor. Additionally, LightZone integrates seamlessly with
Aperture via the [Direct Save](Preferences.html#Preferences-Direct_Save)
preference.

**Note:** The information provided here is accurate as of Aperture
version 1.5.3 but may need to change for future versions.

To configure Aperture to work with LightZone:
---------------------------------------------

1.  Select Aperture \> Preferences... (the Preferences dialog will
    appear) and refer to the Output section.

2.  Under External Image Editor, click the Choose... button.

3.  Navigate to and select LightZone.

4.  For External Editor File Format, select TIFF.

5.  Close the Preferences dialog.

[![image](images/AppleScriptAppIcon.png)](help:runscript='LightZone_Help/scripts/Set_Aperture_Prefs.scpt')
Alternatively, you can click the AppleScript icon to the left to
configure Aperture automatically for you now. (If Aperture is running,
you must quit it first.)

 When using an external editor, Aperture:

1.  Creates a TIFF copy of your photo (with corrections applied).

2.  Adds this TIFF copy to the current Aperture library as a version of
    the original.

3.  Gives this TIFF copy to the external editor to edit.

To configure LightZone to work with Aperture:
---------------------------------------------

1.  Select LightZone \> Preferences... (The Preferences dialog will
    appear.)

2.  Make sure the [Direct
    Save](Preferences.html#Preferences-Direct_Save) check-box is
    checked.

3.  Close the Preferences dialog.

Now that everything has been configured, you can select one of your
photos in Aperture and edit it in LightZone.

To edit your photo in LightZone from Aperture:
----------------------------------------------

1.  In Aperture, select the photo you want to edit.

2.  Select Images \> Open With External Editor. (LightZone will launch
    and open the TIFF copy of your photo.)

3.  In LightZone, edit the TIFF copy of your photo to your liking as you
    would any photo.

4.  Select File \> Save to Aperture. (LightZone will save your edited
    photo back to the TIFF copy overwriting it, switch back to Aperture,
    and Aperture will reload the now edited TIFF copy of your photo.)

### See also:

-   [Multipage TIFF images](Multipage_TIFF.html)
-   [Workflow integration: iPhoto](Integration-iPhoto.html)
-   [Workflow integration: iView
    MediaPro](Integration-iView_MediaPro.html)
-   [Workflow integration: Lightroom](Integration-Lightroom.html)

