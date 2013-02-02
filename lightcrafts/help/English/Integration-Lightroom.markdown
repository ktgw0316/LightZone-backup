Workflow integration: Lightroom
===============================

Photoshop Lightroom (from Adobe Systems) allows you to select another
application as an "external editor" for editing your photos. You can
configure LightZone to be such an editor. Additionally, LightZone
integrates seamlessly with Lightroom via the [Direct
Save](Preferences.html#Preferences-Direct_Save) preference.

**Note:** The information provided here is accurate as of Lightroom
version 1.1 but may need to change for future versions.

To configure Lightroom to work with LightZone:
----------------------------------------------

1.  Select Lightroom Edit \> Preferences...
2.  Click the External Editors tab.
3.  Under Additional External Editor, click the Choose... button.
4.  Navigate to and select LightZone.
5.  For File Format, select TIFF.
6.  For Bit Depth, select 16 bits/component.
7.  Under Edit Externally File Naming, the format must be the default
    setting of Filename-Edit.
8.  Close the Preferences window.

[![image](images/AppleScriptAppIcon.png)](help:runscript='LightZone_Help/scripts/Set_Lightroom_Prefs.scpt')
Alternatively, you can click the AppleScript icon to the left to
configure Lightroom automatically for you now. (If Lightroom is running,
you must quit it first.) \
 When using an external, Lightroom presents you with three choices:

-   Edit Original
-   Edit a Copy
-   Edit a Copy with Lightroom Adjustments

For raw photos, Lightroom only allows the third choice. For JPEG or TIFF
photos, you should select "Edit a Copy with Lightroom Adjustments" to
avoid modifying your original photo. In that case, Lightroom:

1.  Creates a TIFF copy of your photo.
2.  Adds this TIFF copy to the current Lightroom library.
3.  Gives this TIFF copy to the external editor to edit.

To configure LightZone to work with Lightroom:
----------------------------------------------

1.  Select LightZone Edit \> Preferences...
2.  Make sure the [Direct
    Save](Preferences.html#Preferences-Direct_Save) check-box is
    checked.
3.  Close the Preferences dialog.

Now that everything has been configured, you can select one of your
photos in Lightroom and edit it in LightZone.

To edit your photo in LightZone from Lightroom:
-----------------------------------------------

1.  In Lightroom, select the photo you want to edit.
2.  Select Photo \> Edit. (Lightroom will display its Edit Photo dialog
    offering you its three choices.)
3.  If this is an original photo, select "Edit a Copy with Lightroom
    Adjustments." If this is a TIFF copy that was created earlier,
    select "Edit Original." (LightZone will launch and open your photo.)
4.  In LightZone, edit your photo to your liking as you would any photo.
5.  Select File \> Save to Lightroom. (LightZone will save your edited
    photo back to the TIFF copy overwriting it, switch back to
    Lightroom, and Lightroom will reload the now edited TIFF copy of
    your photo.) it.
6.  Switch back to Lightroom: it will reload the now edited version of
    your photo.

**Note:** Depending upon other Lightroom preferences, you may notice an
exclamation point appear on the thumbnail of the TIFF copy of your photo
in Lightroom after you've edited it in LightZone. If you click the
exclamation point, Lightroom will inform you that the metadata for the
photo has been changed by another application and ask you how to
synchronize the data. The safest option is to select Overwrite Settings.

### See also:

-   [Multipage TIFF images](Multipage_TIFF.html)
-   [Workflow integration: Aperture](Integration-Aperture.html)
-   [Workflow integration: iPhoto](Integration-iPhoto.html)
-   [Workflow integration: iView
    MediaPro](Integration-iView_MediaPro.html)

