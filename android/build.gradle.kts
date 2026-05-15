plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

// 작업 폴더가 macOS iCloud Drive 동기화 영역(~/Desktop, ~/Documents,
// ~/Library/Mobile Documents) 안에 있으면, 빌드 산출물(`build/`)을 sync 대상이
// 아닌 ~/Library/Caches 아래로 redirect 한다.
// iCloud의 충돌 회피 정책(같은 파일을 ` 2.ext`, ` 3.ext`로 자동 복제)이 Gradle의
// 다량 임시 파일 IO와 race를 일으켜 빌드를 깨뜨리는 문제를 사전 차단.
// iCloud 외부에서 작업하는 PC에선 자동으로 비활성 → 기본 build/ 사용.
run {
    val home = System.getProperty("user.home")
    val isMacOS = System.getProperty("os.name").lowercase().contains("mac")
    val projectPath = rootDir.absolutePath
    val inICloudSyncedFolder = isMacOS && (
        projectPath.startsWith("$home/Desktop") ||
            projectPath.startsWith("$home/Documents") ||
            projectPath.contains("Mobile Documents/com~apple~CloudDocs")
        )
    if (inICloudSyncedFolder) {
        val externalRoot = file("$home/Library/Caches/arproperty-build")
        subprojects {
            layout.buildDirectory.set(
                file("$externalRoot/${project.path.replace(":", "/").trim('/')}")
            )
        }
        println("[ARProperty] build/ redirected to $externalRoot (iCloud sync 회피)")
    }
}
