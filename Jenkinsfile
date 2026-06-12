pipeline {
    agent any

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Unit Tests') {
            steps {
                dir('backend') {
                    sh './mvnw test'
                }
            }
            post {
                always {
                    junit 'backend/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Integration Tests') {
            steps {
                dir('backend') {
                    sh './mvnw verify -Dspring.profiles.active=integration'
                }
            }
            post {
                always {
                    junit 'backend/target/failsafe-reports/*.xml'
                }
            }
        }

        stage('Build') {
            steps {
                dir('backend') {
                    sh './mvnw clean package -DskipTests'
                }
                dir('frontend') {
                    sh 'npm ci --legacy-peer-deps'
                    sh 'npm run build'
                }
            }
        }

        stage('Docker Deploy') {
            when {
                branch 'main'
            }
            steps {
                sh 'docker compose build'
                sh '''
                    if [ -n "${RENDER_DEPLOY_HOOK_URL}" ]; then
                        curl -s -X POST "${RENDER_DEPLOY_HOOK_URL}"
                        echo "Render deploy triggered."
                    fi
                '''
            }
        }
    }

    post {
        failure {
            echo 'Pipeline failed — check test reports above.'
        }
        success {
            echo 'All stages passed.'
        }
    }
}
