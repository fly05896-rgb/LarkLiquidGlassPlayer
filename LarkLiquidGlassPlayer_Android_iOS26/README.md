# 💧 Lark Liquid Glass Video Player (iOS 26 Edition)

تطبيق مشغل فيديوهات أندرويد فائق السرعة مبني بلغة **Kotlin 2.1 (K2)** و **Jetpack Compose** و **AndroidX Media3 (ExoPlayer)** مع محرك **الزجاج السائل الحقيقي (iOS 26 Liquid Glass)** باستخدام مكتبة [`io.github.kyant0:backdrop:2.0.1`](https://github.com/Kyant0/AndroidLiquidGlass) ومكتبة [`io.github.kyant0:capsule:2.1.2`](https://github.com/Kyant0/Capsule) وشيدر **AGSL `RuntimeShader`** المسرّع عبر المعالج الرسومي (GPU).

---

## ✨ أبرز المميزات الهندسية والبصرية

1. **تأثير الزجاج السائل المطابق لـ iOS 26 بالملي (`LiquidGlassEngine.kt`)**:
   - **انكسار بصري حقيقي (Real SDF Optical Lens Refraction)**: يثني المشهد الخلفي للفيديو تحت الأزرار الدائرية والكبسولات تماماً مثل عدسة قطرة الماء (`lens(refractionHeight, refractionAmount, depthEffect = true, chromaticAberration = true)`).
   - **تشتت لوني طيفي (Chromatic Dispersion)**: فصل طفيف لقنوات RGB عند حواف الزجاج المنحنية مع لمعان كريستالي (`Highlight` + `InnerShadow`).
   - **فيزياء مائية تفاعلية عند اللمس والسحب (`LiquidInteractivePhysics`)**: عند الضغط على أي زر يرتفع للأعلى (`translationY = -liftHeight`) ويتمدد بنابض فيزيائي مرن (`spring(dampingRatio = 0.48f, stiffness = 380f)`) ويتحرك مع إصبعك بسلاسة مغناطيسية (`tanh` damping + directional stretch).
2. **انكسار القائمة الرئيسية أثناء تقليب الفيديوهات (`HomeLibraryScreen.kt`)**:
   - تمر الفيديوهات والصور المصغرة تحت شريط البحث الزجاجي العائم، فلاتر التصنيفات، وشريط التنقل السفلي، مع شيدر موجي يتفاعل مع سرعة التمرير (`scrollVelocity`).
3. **تصميم مشغل iOS 26 الكامل + مميزات Lark Video (`Ios26VideoPlayerScreen.kt`)**:
   - **أعلى اليسار**: زر الإغلاق الدائري (`✕`) + كبسولة تضم وضع نافذة داخل نافذة (`PiP`)، تغيير أبعاد الشاشة (`Fit / Fill Crop / 16:9 Stretch`)، وقفل اللمس (`Screen Lock`).
   - **أعلى اليمين**: كبسولة مستوى الصوت التفاعلية المزودة بشريط سحب سائل وأيقونة السماعة.
   - **منتصف الشاشة**: 3 عدسات زجاجية دائرية (رجوع `15s`/`10s`، زر تشغيل/إيقاف مركزي كبير، تقديم `15s`/`10s`) تكسر إطارات الفيديو الحية تحتها عبر `TextureView` + `Modifier.layerBackdrop(videoBackdrop)`.
   - **يمين الشريط السفلي**: كبسولة موجات الصوت/الإيكولايزر (`|||`) والترجمة/السرعة (`CC`).
   - **أسفل الشاشة**: كبسولة شريط التقدم الكامل مع الوقت المنقضي على اليسار (`16:50`) والوقت المتبقي بالسالب على اليمين (`-42:10`).
   - **محرك الصوت المحيطي ومضخم الصوت 200% (`AudioEqualizerEngine.kt`)**: يدمج `LoudnessEnhancer` لرفع الصوت حتى **200%** مع `Equalizer` خماسي الترددات و `Virtualizer` للصوت الثلاثي الأبعاد.

---

## 🚀 طريقة بناء التطبيق وتحميل الـ APK عبر GitHub Actions

1. أنشئ مستودعاً جديداً (New Repository) على حسابك في **GitHub**.
2. ارفع جميع ملفات هذا المشروع إلى المستودع (تأكد من وجود ملف `.github/workflows/build-apk.yml`).
3. ادخل إلى تبويب **Actions** في مستودعك على GitHub، ستجد عملية البناء **`Build Lark Liquid Glass iOS 26 APK`** تعمل تلقائياً (أو اضغط **Run workflow**).
4. بعد انتهاء البناء (حوالي دقيقتين إلى 3 دقائق)، انزل لأسفل صفحة البناء تحت قسم **Artifacts** وحمّل ملف **`LarkLiquidGlass-iOS26-Player-APKs`** الذي يحتوي على:
   - `LarkLiquidGlass-iOS26-Release.apk` (نسخة نهائية موقعة وجاهزة للتثبيت فوراً).
   - `LarkLiquidGlass-iOS26-Debug.apk` (نسخة التطوير).
