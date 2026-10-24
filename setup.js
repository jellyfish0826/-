// 사용법:  node setup.js   (GitHub Actions에서 자동 실행됨)
const { execSync } = require("child_process");
const fs = require("fs");
const path = require("path");
const R = (...p) => path.join(__dirname, ...p);
const run = (cmd, cwd) => { console.log("\n> " + cmd); execSync(cmd, { stdio: "inherit", cwd: cwd || __dirname, shell: true }); };
const cp = (a, b) => { fs.mkdirSync(path.dirname(b), { recursive: true }); fs.copyFileSync(a, b); };

// 0) 평평하게 올린 파일들을 www/ 구조로 배치
cp(R("index.html"), R("www/index.html"));
cp(R("pdf.min.js"), R("www/pdfjs/pdf.min.js"));
cp(R("pdf.worker.min.js"), R("www/pdfjs/pdf.worker.min.js"));

run("npm install");
if (!fs.existsSync(R("android"))) run("npx cap add android");

// 1) 갤러리 저장용 MainActivity 덮어쓰기
cp(R("MainActivity.java"), R("android/app/src/main/java/com/spreadcomposer/app/MainActivity.java"));

// 2) minSdk 29 (Android 10+)
const vg = R("android/variables.gradle");
fs.writeFileSync(vg, fs.readFileSync(vg, "utf8").replace(/minSdkVersion\s*=\s*\d+/, "minSdkVersion = 29"));

run("npx cap sync android");

const sdk = process.env.ANDROID_HOME || process.env.ANDROID_SDK_ROOT;
if (sdk) {
  const g = process.platform === "win32" ? "gradlew.bat" : "./gradlew";
  run(g + " assembleDebug", R("android"));
  console.log("\n✅ APK: android/app/build/outputs/apk/debug/app-debug.apk");
} else {
  console.log("\n✅ 준비 완료. npx cap open android 로 Android Studio에서 빌드하세요.");
}
