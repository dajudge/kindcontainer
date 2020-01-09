node {
        stage("Checkout") {
                checkout scm
        }

        stage("Build") {
                sh "./workbench ./gradlew clean build"
        }
}
