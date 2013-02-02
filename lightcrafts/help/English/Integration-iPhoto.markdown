Workflow integration: iPhoto
============================

iPhoto (from Apple) allows you to select another application as an
"external editor" for editing your photos. You can configure LightZone
to be such an editor. Additionally, LightZone integrates seamlessly with
iPhoto via the [Direct Save](Preferences.html#Preferences-Direct_Save)
preference.

**Note:** The information provided here is accurate as of iPhoto version
7.0.1 but may need to change for future versions.

To configure iPhoto to work with LightZone:
-------------------------------------------

1.  Select iPhoto \> Preferences... (the Preferences dialog will
    appear).
2.  Select the "General" tab.
3.  For Edit Photo, select "In application..."
4.  Navigate to and select LightZone.
5.  Select the "Advanced" tab.
6.  Under RAW Photos, check "Use RAW files with external editor."
7.  Close the Preferences dialog.

[![image](images/AppleScriptAppIcon.png)](help:runscript='LightZone_Help/scripts/Set_iPhoto_Prefs.scpt')
Alternatively, you can click the AppleScript icon to the left to
configure iPhoto automatically for you now. (If iPhoto is running, you
must quit it first.)

 When using an external editor, iPhoto gives your original photo to the
external editor to edit.

To configure LightZone to work with iPhoto:
-------------------------------------------

1.  Select LightZone \> Preferences... (the Preferences dialog will
    appear).
2.  Make sure the [Direct
    Save](Preferences.html#Preferences-Direct_Save) check-box is
    checked.
3.  Close the Preferences dialog.

Now that everything has been configured, you can select one of your
photos in iPhoto and edit it in LightZone.

To edit your photo in LightZone from iPhoto:
--------------------------------------------

1.  In iPhoto, double-click the photo you want to edit. (LightZone will
    launch and open your photo.)

2.  In LightZone, edit your photo to your liking as you would any photo.
3.  Select File \> Save to iPhoto. (LightZone will automatically save
    your edited photo, switch to iPhoto, and tell iPhoto to import it
    into the current album.)

### See also:

-   [Workflow integration: Aperture](Integration-Aperture.html)
-   [Workflow integration: iView
    MediaPro](Integration-iView_MediaPro.html)
-   [Workflow integration: Lightroom](Integration-Lightroom.html)

