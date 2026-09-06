# Gallery photos

Each album is a folder here, named to match the `id` in
`src/lib/content/gallery.ts`. Drop the images in and they appear on `/gallery`.

    public/gallery/<album-id>/01.jpg
    public/gallery/<album-id>/02.jpg

The filenames must match the `photos[].src` paths in that file. Any photo that is
not present renders as a "Photo to be added" tile rather than a broken image, so
an album can be written up before its photos are collected.

Keep images reasonably sized — these are served as-is, so a 6 MB phone photo is
a 6 MB download. Around 1600px on the long edge is plenty.

Update the `alt` text when adding real photos: it should describe what is in the
picture, not repeat the event name.
