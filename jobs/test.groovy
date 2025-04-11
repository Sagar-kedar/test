pipelineJob('MULTI_CHOICE') {
    definition {
        cps {
            script("""
            pipeline{
                agent {
                    kubernetes {
                        label 'jenkins-ubuntu'
                        defaultContainer 'ubuntu'
                    }
                }
            
                parameters {
                    activeChoiceParam('SERVICE_NAME') {                                     // Active choice parameter to select multiple services
                        description('Select the service name(s) for deployment')
                        filterable()                                                        // Allows filtering in the UI
                        choiceType('MULTI_SELECT')                                          // Multi-select checkbox
                        groovyScript {
                            script('return ["accesspoint3-21", "studio", "proxy", "client", "krista-ai-server", "client-beta", "platform", "accesspoint3"]')
                        }
                    }
                    activeChoiceParam('NAMESPACE') {                                     // Active choice parameter to select namespace
                        description('Select the namespace for deployment')
                        filterable()                                                        // Allows filtering in the UI
                        choiceType('SINGLE_SELECT')                                          // SINGLE-select checkbox
                        groovyScript {
                            script('return ["uslab-01-eng-krista-app", "uslab-02-eng-krista-app", "uslab-03-eng-krista-app", "uslab-04-eng-krista-app", "uslab-05-eng-krista-app", "uslab-qa-eng-krista-app"]')
                        }
                    }
                    // Other parameters
                    string(name: 'HELM_VERSION', defaultValue: '', description: 'Version of Helm to deploy')
                }
                environment {
                    HELM_VERSION = "${params.HELM_VERSION}"
                    TEAMS_WEBHOOK_URL = credentials('teams-webhook-url')
                }
                stages {
                    stage('Set Build Display Name') {
                        steps {
                            script {
                                // Get the username of the person who triggered the build
                                def username = currentBuild.getBuildCauses().find { it?.userId }?.userName ?: 'Unknown User'

                                // Set the build display name
                                currentBuild.displayName = "#${env.BUILD_NUMBER} - ${params.NAMESPACE} - Triggered by ${username}"
                            }
                        }
                    }
                    stage('Checkout Repositories') {
                        steps {
                            script {
                                // Handle 'environments' repository
                                dir("environments-engineering") {
                                    git branch: 'master',
                                        credentialsId: 'bitbucket-creds',
                                        url: 'git_url'
                                }
                                dir("applications") {
                                    checkout([
                                        $class: 'GitSCM',
                                        branches: [[name: "refs/tags/${params.HELM_VERSION}"]],
                                        doGenerateSubmoduleConfigurations: false,
                                        extensions: [],
                                        userRemoteConfigs: [[
                                            url: 'url',
                                            credentialsId: 'bitbucket-creds'
                                        ]]
                                    ])
                                }
                            }
                        }        
                    }
                    stage('Deploy Helm Charts') {
                        steps {
                            script {
                                // Get the selected services from the parameter
                                def selectedServices = params.SERVICE_NAME
                                
                                // Loop through the selected services and deploy their Helm charts
                                selectedServices.each { service ->
                                    echo "Deploying Helm chart for ${service}"

                                    // Example Helm command for each service
                                    sh "helm upgrade --install ${service} ./charts/${service}"
                                }
                            }
                        }
                    }
                }

            }
            """)
        }
    }
}