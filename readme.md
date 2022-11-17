# MyExifSorter
This little tool helps you organize your pictures using [EXIF](https://de.wikipedia.org/wiki/Exchangeable_Image_File_Format) tags.
Simply choose source and destination folder and click on the copy button. All pictures will be sorted and copied to destination folder. That's it 🥳🥳🥳.
In case a picture date cannot be determined it is placed in the following folder:
```
1970\January
```

## Build the project
```
mvn clean package
```
The final images can then be found at:

### Windows:

```
\target\jlink-image\bin\myexifsorter.bat
```
or
```
\target\jpackage\myexifsorter\myexifsorter.exe
```

### MacOS:

```
\target\jlink-image\bin\myexifsorter
```
or
```
\target\jpackage\myexifsorter\myexifsorter.app
```

For licence info, see: [Licence.md](Licence.md)