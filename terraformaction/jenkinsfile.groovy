pipeline {
    agent any
    
    environment{
        AZURE_SP = credentials('azure-sp')
        
        ARM_SUBSCRIPTION_ID = 'bd95261f-11c6-4825-949b-23c9b568b7f6'
        ARM_TENANT_ID = '10497c69-15e5-4cb1-8aa1-3b7dacd2163d'
        ARM_CLIENT_SECRET = "${AZURE_SP_PSW}"
        ARM_CLIENT_ID = "${AZURE_SP_USR}"
        
        GIT_CREDS = credentials('git-token')
        GIT_USERNAME = "${GIT_CREDS_USR}"
        GIT_TOKEN = "${GIT_CREDS_PSW}"
    }
    
    stages {
        stage('Clean Workspace'){
            steps{
                cleanWs()
            }
        }
        stage('Git Checkout'){
            steps{
                checkout([$class: 'GitSCM', branches: [[name: '*/master']], extensions: [], userRemoteConfigs: [[url: 'https://github.com/umitdemirtas/azure-terraform-configuration']]])
            }
        }       
        stage('Environment Check'){
            steps{
                sh """
                    echo Subscription ID: $ARM_SUBSCRIPTION_ID
                    echo Tenant ID: $ARM_TENANT_ID
                    echo Client ID: $ARM_CLIENT_ID
                """
            }
        }
        stage('New VM Configuration Generator') {
            steps {
                script {
                    def currentConfig = readFile file: 'main.tf'
                     
                    dir("Templates"){
                        def template = readFile file: 'ubuntu.tf'
                        template = template.replace("VM_NAME", "${VM_NAME}")
                        template = template.replace("LOCATION", "${LOCATION}")
                        template = template.replace("ADMIN_USERNAME", "${ADMIN_USERNAME}")
                        
                        currentConfig = currentConfig + '\n' + template                       
                        
                        println(currentConfig)
                    }

                    writeFile file: 'main.tf', text: currentConfig
                }
            }
        }
        stage ("Terraform Init") {
            steps {
                sh ('terraform init -reconfigure') 
            }
        }
        stage ("Terraform Plan") {
            steps {
                sh ('terraform plan') 
            }
        }
                
        stage ("Terraform Action") {
            steps {
                echo "Terraform action is --> ${ACTION}"
                // sh ('terraform ${ACTION} --auto-approve') 
           }
        }
        stage ("Git Commit") {
            steps {
                script {
                    sh "git config user.email ${GIT_USERNAME}"
                    sh "git config user.name ${GIT_USERNAME}"
                    //sh "git config --global push.default matching"
                    sh "git checkout master"
                    
                    sh "git add ."
                    // sh "git diff-index --quiet HEAD"
                    sh "git commit -m '${VM_NAME} add configuration'"
                    //sh "git push --set-upstream origin master"

                    sh "git push https://${GIT_USERNAME}:${GIT_TOKEN}@github.com/umitdemirtas/azure-terraform-configuration.git"
                }
           }
        }
    }
}
