plugins { java }
group="com.hardcraft";version="0.1.1"
repositories { mavenCentral();maven("https://repo.papermc.io/repository/maven-public/") }
dependencies { compileOnly("io.papermc.paper:paper-api:26.2.build.129-stable");compileOnly(files("../HardCraftCore/build/libs/HardCraftCore-0.1.1.jar"));compileOnly(files("../HardCraftItems/build/libs/HardCraftItems-0.1.0.jar"));implementation("org.xerial:sqlite-jdbc:3.50.3.0");testImplementation(platform("org.junit:junit-bom:5.13.4"));testImplementation("org.junit.jupiter:junit-jupiter");testRuntimeOnly("org.junit.platform:junit-platform-launcher") }
java { toolchain.languageVersion.set(JavaLanguageVersion.of(25)) }
tasks.withType<JavaCompile>().configureEach { options.encoding="UTF-8";options.release.set(25) }
tasks.processResources { filesMatching("plugin.yml") { expand("version" to project.version) } }
tasks.jar { archiveFileName.set("HardCraftMobs-${project.version}.jar");duplicatesStrategy=DuplicatesStrategy.EXCLUDE;from(configurations.runtimeClasspath.get().map { if(it.isDirectory)it else zipTree(it) }) }
tasks.test { useJUnitPlatform() }