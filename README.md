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
