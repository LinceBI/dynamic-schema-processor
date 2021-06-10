plugins {
	id("java-library")
	id("maven-publish")
}

java.sourceCompatibility = JavaVersion.VERSION_1_8
java.targetCompatibility = JavaVersion.VERSION_1_8

version = "${project.property("version")}${project.property("versionSuffix")}"

repositories {
	mavenCentral()
	maven { url = uri("https://repo.stratebi.com/repository/pentaho-mvn/") }
}

dependencies {
	compileOnly("pentaho:pentaho-platform-api:8.3.0.23-1295") { isTransitive = false }
	compileOnly("pentaho:pentaho-platform-core:8.3.0.23-1295") { isTransitive = false }
	compileOnly("pentaho:mondrian:8.3.0.23-1295") { isTransitive = false }
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
