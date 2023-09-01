plugins {
	id("java-library")
	id("maven-publish")
	id("com.netflix.nebula.dependency-lock") version "13.3.0"
}

java.sourceCompatibility = JavaVersion.VERSION_1_8
java.targetCompatibility = JavaVersion.VERSION_1_8

version = "${project.property("version")}${project.property("versionSuffix")}"

repositories {
	mavenCentral()
	maven { url = uri("https://repo.stratebi.com/repository/pentaho-mvn/") }
}

dependencies {
	compileOnly("pentaho:pentaho-platform-api:9.3.0.5-753") { isTransitive = false }
	compileOnly("pentaho:pentaho-platform-core:9.3.0.5-753") { isTransitive = false }
	compileOnly("pentaho:mondrian:9.3.0.5-753") { isTransitive = false }
}

publishing {
	publications {
		create<MavenPublication>("maven") {
			groupId = "${project.property("group")}"
			artifactId = "${project.property("artifact")}"
			version = "${project.property("version")}"
			from(components["java"])
		}
	}

	repositories {
		maven {
			val releasesRepoUrl = "https://repo.stratebi.com/repository/lincebi-mvn-releases/"
			val snapshotsRepoUrl = "https://repo.stratebi.com/repository/lincebi-mvn-snapshots/"
			url = uri(if ("${project.property("versionSuffix")}".endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
			credentials {
				username = System.getenv("REPO_MAVEN_LINCEBI_RW_USER") ?: ""
				password = System.getenv("REPO_MAVEN_LINCEBI_RW_PASSWORD")?: ""
			}
		}
	}
}
