1:plugins {
2:    id("com.android.application")
3:    id("org.jetbrains.kotlin.android")
4:    id("com.google.devtools.ksp")
5:    id("com.google.dagger.hilt.android")
6:    id("jacoco")
7:    kotlin("kapt") // Needed for Hilt with kapt in tests
8:}
9:
10:android {
11:    namespace = "com.pennywise.app"
12:    compileSdk = 34
13:
14:    defaultConfig {
15:        applicationId = "com.pennywise.app"
16:        minSdk = 26
17:        targetSdk = 34
18:        versionCode = 1
19:        versionName = "1.0"
20:
21:        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
22:        vectorDrawables.useSupportLibrary = true
23:    }
24:
25:    buildTypes {
26:        release {
27:            isMinifyEnabled = false
28:            proguardFiles(
29:                getDefaultProguardFile("proguard-android-optimize.txt"),
30:                "proguard-rules.pro"
31:            )
32:        }
33:    }
34:
35:    compileOptions {
36:        sourceCompatibility = JavaVersion.VERSION_17
37:        targetCompatibility = JavaVersion.VERSION_17
38:    }
39:    kotlinOptions {
40:        jvmTarget = "17"
41:    }
42:
43:    buildFeatures {
44:        compose = true
45:        viewBinding = true
46:        dataBinding = true
47:    }
48:
49:    composeOptions {
50:        kotlinCompilerExtensionVersion = "1.5.8"
51:    }
52:
53:    packaging {
54:        resources {
55:            excludes += "/META-INF/{AL2.0,LGPL2.1}"
56:        }
57:    }
58:
59:    testOptions {
60:        unitTests.isIncludeAndroidResources = true
62:    }
63:}
64:
65:dependencies {
66:    // Core
67:    implementation("androidx.core:core-ktx:1.12.0")
68:    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
69:    implementation("androidx.activity:activity-compose:1.8.2")
70:
71:    // Compose
72:    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
73:    implementation("androidx.compose.ui:ui")
74:    implementation("androidx.compose.ui:ui-graphics")
75:    implementation("androidx.compose.ui:ui-tooling-preview")
76:    implementation("androidx.compose.material3:material3")
77:    implementation("androidx.compose.material:material-icons-extended")
78:
79:    // Material XML
80:    implementation("com.google.android.material:material:1.11.0")
81:
82:    // Navigation
83:    implementation("androidx.navigation:navigation-compose:2.7.7")
84:
85:    // Lifecycle
86:    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
87:    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
88:
89:    // Room
90:    implementation("androidx.room:room-runtime:2.6.1")
91:    implementation("androidx.room:room-ktx:2.6.1")
92:    ksp("androidx.room:room-compiler:2.6.1")
93:
94:    // DataStore
95:    implementation("androidx.datastore:datastore-preferences:1.0.0")
96:
97:    // Biometric
98:    implementation("androidx.biometric:biometric:1.2.0-alpha05")
99:
100:    // Coroutines
101:    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
102:    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
103:
104:    // Hilt
105:    implementation("com.google.dagger:hilt-android:2.48")
106:    kapt("com.google.dagger:hilt-android-compiler:2.48")
107:    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
108:
109:    // Networking
110:    implementation("com.squareup.retrofit2:retrofit:2.9.0")
111:    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
112:    implementation("com.squareup.okhttp3:okhttp:4.12.0")
113:    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
114:
115:    // Logging
116:    implementation("com.jakewharton.timber:timber:5.0.1")
117:
118:    // Unit Testing
119:    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
120:    testImplementation("io.mockk:mockk:1.13.8")
121:    testImplementation("org.mockito.kotlin:mockito-kotlin:4.1.0")
122:    testImplementation("org.mockito:mockito-core:5.6.0")
123:    testImplementation("org.robolectric:robolectric:4.10.3")
124:    testImplementation("androidx.test:core:1.5.0")
125:    testImplementation("androidx.test:core-ktx:1.5.0")
126:    testImplementation("androidx.test.ext:junit:1.1.5")
127:    testImplementation("androidx.test.ext:junit-ktx:1.1.5")
128:    testImplementation("androidx.compose.ui:ui-test-junit4")
129:    testImplementation("androidx.room:room-testing:2.6.1")
130:    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
131:    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
132:
133:    // Instrumented Tests
134:    androidTestImplementation("androidx.test.ext:junit:1.1.5")
135:    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
136:    androidTestImplementation("androidx.test.espresso:espresso-accessibility:3.5.1")
137:    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.5.1")
138:    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))
139:    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
140:    androidTestImplementation("androidx.compose.ui:ui-test-manifest")
141:    androidTestImplementation("com.google.dagger:hilt-android-testing:2.48")
142:    kaptAndroidTest("com.google.dagger:hilt-android-compiler:2.48")
143:
144:    // Debug
145:    debugImplementation("androidx.compose.ui:ui-tooling")
146:    debugImplementation("androidx.compose.ui:ui-test-manifest")
147:
148:    // Benchmark
149:    androidTestImplementation("androidx.benchmark:benchmark-junit4:1.2.2")
150:}
151:
152:jacoco {
153:    toolVersion = "0.8.13"
154:}
155:
156:tasks.withType<Test> {
157:    useJUnitPlatform() // Ensures JUnit5 runs
158:    jvmArgs = listOf("-Xmx1024m", "-XX:+UseG1GC", "-XX:+UseStringDeduplication")
159:    configure<JacocoTaskExtension> {
160:        isIncludeNoLocationClasses = true
161:        excludes = listOf(
162:            "jdk.*", "sun.*", "com.sun.*",
163:            "org.robolectric.*", "androidx.test.*", "io.mockk.*",
164:            "org.junit.*", "org.mockito.*", "androidx.compose.ui.test.*",
165:            "androidx.test.espresso.*"
166:        )
167:    }
168:}
169:
170:tasks.register<JacocoReport>("jacocoTestReport") {
171:    dependsOn("testDebugUnitTest")
172:
173:    reports {
174:        xml.required.set(true)
175:        html.required.set(true)
176:    }
177:
178:    val fileFilter = listOf("**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*")
179:    val debugTree = fileTree("${project.buildDir}/tmp/kotlin-classes/debug") {
180:        exclude(fileFilter)
181:    }
182:    val mainSrc = "${project.projectDir}/src/main/java"
183:
184:    sourceDirectories.setFrom(files(mainSrc))
185:    classDirectories.setFrom(files(debugTree))
186:    executionData.setFrom(fileTree(project.buildDir) {
187:        include("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
188:    })
189:}
