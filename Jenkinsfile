pipeline {
  agent any
  environment {
    // Specified for Jenkins server
    JAVA_HOME = "C:/Program Files/Android/Android Studio/jre"
    ANDROID_SDK_ROOT = "C:/Android/Sdk"
    GRADLE_USER_HOME = "C:/gradle-cache"
    KEYSTORE_LOCATION = credentials('nitspec-keystore-location')
  }
  options {
    // Stop the build early in case of compile or test failures
    skipStagesAfterUnstable()
  }
  stages {
    stage('Compile') {
      steps {
        dir('Android') {
            bat './gradlew compileDebugSources'
        }
      }
    }
    stage('Unit test') {
      steps {
        dir('Android') {
            bat './gradlew testDebugUnitTest testDebugUnitTest'
            junit '**/TEST-*.xml' // Enable later when having unit tests to report
        }
      }
    }
    stage('Build APK') {
      steps {
        dir('Android') {
            bat './gradlew assembleDebug'
        }
      }
    }
    stage('Deploy') {
      when {
        // Only execute this stage when building from the `master` branch
        branch 'master'
      }
      steps {
        dir('Android') {
            // Execute bundle release build
            bat './gradlew :app:bundleRelease'

            // Sign bundle
            withCredentials([string(credentialsId: 'nitspec-signing-password', variable: 'nitspec-signing-password')]) {
                bat 'jarsigner -verbose -keystore %KEYSTORE_LOCATION% %WORKSPACE%\\Android\\app\\build\\outputs\\bundle\\release\\app-release.aab Nitramite --storepass "%nitspec-signing-password%"'
            }

            // Archive the AAB (Android App Bundle) so that it can be downloaded from Jenkins
            archiveArtifacts '**/bundle/release/*.aab'
        }
      }
    }
  }
}
