pipeline {
    agent none
    triggers {
        pollSCM('H/5 * * * *')
    }
    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '10'))
        ansiColor('xterm')
        timeout(time: 1, unit: 'HOURS')
        skipDefaultCheckout()
    }
    stages {
        stage('Build') {
            agent {
                docker {
                    label 'docker && infinera'
                    image 'se-artif-prd.infinera.com/gradle:4.3.1'
                }
            }
            steps {
                checkout scm
                stash 'checkout'
                stash name: 'checkout', useDefaultExcludes: false
                sh 'gradle assemble'
                stash name: 'assemble', useDefaultExcludes: false
            }
            post {
                always {
                    cleanWs()
                }
            }
        }
        stage('Test') {
            // parallelize unit tests / api tests?
            agent {
                docker {
                    label 'docker'
                    image 'se-artif-prd.infinera.com/gradle:4.3.1'
                }
            }
            steps {
                unstash 'assemble'
                sh 'gradle test'
            }
            post {
                always {
                    junit '**/build/test-results/junit-platform/TEST-*.xml'
                    cleanWs()
                }
            }

        }
        stage('Publish') {
            agent {
                docker {
                    label 'docker'
                    image 'se-artif-prd.infinera.com/gradle:4.3.1'
                }
            }
            steps {
                unstash 'assemble'
                script {
                    GIT_COMMIT = sh (script: 'git --no-pager show -s --format=\'%h\'', returnStdout: true).trim().take(7)
                    withEnv(["TAG=1.0.0-$GIT_COMMIT"]) {
                        sh "./gradlew artifactoryPublish -PpartOfLatestCommitHash=$GIT_COMMIT"
                    }
                }
            }
            post {
                always {
                    cleanWs()
                }
            }
        }
    }
}
