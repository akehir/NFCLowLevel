# Use this plugin

## Limitations

This plugin is meant to be used as an ionic native plugin. The following description works for using it inside your ionic app.

The plugin only works for android.

## Features

The plugin enables you to use the NFC interface of your android phone to communicate with an NFC Type A tag. Other types are not implemented, but could be added easily following the guide below.

## Usage

```typescript
// data to send
const data = new Uint8Array([/*some byte*/, /*some byte*/ , /*...*/ ];

// on event detection
this.nfc.addTagDiscoveredListener().subscribe(event => {
    // ignore init event
    console.log('Tag detected: ' + JSON.stringify(event));
    if (event === 'init') {
        console.log('got init event, tagDiscoveredListener is now initialized');
        return;
        this.nfc.connect().then(()=>{
            console.log('connected');
            this.nfc.transceive(data).then((response)=>{
                console.log('got ' + response ' from tag');
                this.nfc.close().then(()=>{
                    console.log('connection closed again');
                })
            })
        });
    }
```
If you invoke `addTagDiscoveredListener` you will get an observable as a return value which will first emit an string `"init"`once it is initialized and then emit JSON objects with information about the detected tags.

The functions `connect`, `transceive` and `close` more or less directly invoke the underlying android API, as described in their [docs](https://developer.android.com/reference/android/nfc/tech/NfcA).

# Add it to an ionic project

Add this cordova plugin to your project with the following command:

```bash
ionic cordova plugin add https://github.com/noahzarro/NFCLowLevel.git
```

Since it is no official plugin you have to add the `ionic native` wrapper manually. To do that just copy the folder `nfc-low-level` from [this repository](https://github.com/noahzarro/NFCLowLevelIonicNative/tree/master/dist) to the folder `node_modules/@ionic-native`. Then you should be able to use it like a normal ionic native plugin.

# Minimal guide for plugin development

## Create cordova plugin

https://gist.github.com/bsorrentino/68b4589c31cbe96311e597eba73b776c

Develop cordova plugin and push it to github (or local file system)  
Add it to app with:

```bash
ionic cordova plugin add <github_link/file_path>
```

## Create ionic native wrapper

You need to install `gulp` first.  
https://github.com/ionic-team/ionic-native/blob/master/DEVELOPER.md

Clone the whole ionic native repo. Write wrapper for plugin (single index.ts file) and run `npm run build`

copy the generated plugin folder in the dist folder to your app at `\node_modules\@ionic-native\`

Then you can use it the same way as a normal plugin ([like described here](https://ionicframework.com/docs/native/community)). Just add it in `app.module.ts` and in the module where you use it.

# Credit

The code mostly was taken from [this plugin](https://github.com/chariotsolutions/phonegap-nfc). But it did not (or no longer) support NfcA, so I wrote my own little plugin. But without it, it would have been a much harder task, so a big thank you to you guys.