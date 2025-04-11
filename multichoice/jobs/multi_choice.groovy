pipelineJob('test') {
    definition {
        cps {
            script("""
            pipeline {
                agent {
                    kubernetes {
                        label 'jenkins-ubuntu'
                        defaultContainer 'ubuntu'
                    }
                }
                parameters {
                    
                    // Choice Parameter for Cluster selection
                    choice(name: 'CLUSTER', choices: 'kubeadm-us', description: 'Select the Kubernetes cluster')

                    // String Parameter: User enters the global catalog version
                    string(name: 'GLOBAL_CATALOG_VERSION', defaultValue: '', description: 'E.g. GC-1.0.33')

                    // Choice Parameter: Users select helm version from a dropdown
                    choice(name: 'HELM_VERSION', choices: ['2.0.12', '2.0.11', '2.0.10', '2.0.9', '2.0.8', '2.0.10-test', '2.0.7', '2.0.5-jmx', '2.0.4', '2.0.3', '2.0.2', '2.0.1', '2.0.0'], description: 'Select the Helm version to use')

                    // Choice Parameter: Users select ther namespace from a dropdown
                    choice(name: 'NAMESPACE', choices: ['uslab-01-eng-krista-app', 'uslab-02-eng-krista-app', 'uslab-03-eng-krista-app', 'uslab-04-eng-krista-app', 'uslab-05-eng-krista-app', 'uslab-qa-eng-krista-app'], description: 'Select the namespace for deployment')
                    
                    // Active Choice Parameter: Dynamically generate list of services
                    activeChoiceReactiveParam('SERVICE_NAME') {
                        description('Select the service name for deployment')
                        choiceType('CHECKBOX')
                        groovyScript {
                            script {
                                return ["accesspoint3-21", "studio", "proxy", "client", "krista-ai-server", "client-beta", "platform", "accesspoint3"]
                            }
                        }
                    }

                    // String Parameter: User enters the service version
                    string(name: 'SERVICE_VERSION', defaultValue: '', description: 'E.g. 2.2.5-sp1 or 3.0.0')

                }

                environment {
                    HELM_VERSION = "${params.HELM_VERSION}"
                    //TEAMS_WEBHOOK_URL = credentials('teams-webhook-url')
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
                                        url: 'https://devops_build2@bitbucket.org/syncappinc/environments-engineering.git'
                                }

                                // Handle 'applications' repository
                                dir("applications") {
                                    checkout([
                                        $class: 'GitSCM',
                                        branches: [[name: "refs/tags/${params.HELM_VERSION}"]],
                                        doGenerateSubmoduleConfigurations: false,
                                        extensions: [],
                                        userRemoteConfigs: [[
                                            url: 'https://devops_build2@bitbucket.org/syncappinc/applications.git',
                                            credentialsId: 'bitbucket-creds'
                                        ]]
                                    ])
                                }
                            }
                        }
                    }
                    stage('Deploy helm chart') {
                        steps {
                            script {
                                // sh """
                                // set -e
                                // set -x
                                // export KUBECONFIG=${WORKSPACE}/environments-engineering/container-platforms/kubeadm-us/kubeconfig.yaml
                                
                                // helm upgrade --install ${params.SERVICE_NAME} ${WORKSPACE}/applications/krista-app/${params.SERVICE_NAME} \
                                //     --set image.tag=${params.SERVICE_VERSION} \
                                //     --set catalogTag=${params.GLOBAL_CATALOG_VERSION} \
                                //     -f ${WORKSPACE}/environments-engineering/container-platforms/${params.CLUSTER}/${params.NAMESPACE}/krista-appliance/${params.SERVICE_NAME}/values.yaml \
                                //     -n ${params.NAMESPACE}
                                // """
                                def apps = params.SERVICE_NAME ? params.SERVICE_NAME.split(',') : [] // Convert selected checkboxes to a list
                                
                                echo "Selected apps for deployment: ${apps}"  // for debug
                                
                                if (apps.length == 0) {
                                    echo "No applications selected for deployment"
                                    return
                                }
                                
                                    for (app in apps) {
                                        
                                        sh """
                                        set -e
                                        set -x
                                        echo "Deploying : ${app}"
                                        
                                        export KUBECONFIG=${WORKSPACE}/environments-engineering/container-platforms/kubeadm-us/kubeconfig.yaml
                                
                                        helm template ${app} ${WORKSPACE}/applications/krista-app/${app} \
                                            -f ${WORKSPACE}/environments-engineering/container-platforms/${params.CLUSTER}/${params.NAMESPACE}/krista-appliance/${app}/values.yaml
                                        
                                        echo "GC version of ${app} is ${GLOBAL_CATALOG_VERSION}"
                                        
                                        echo "Helm version of ${app} is ${HELM_VERSION}"
                                        
                                        echo "Processing ${app} in ${params.NAMESPACE}"
                                        
                                        echo "Service version of ${app} is ${params.SERVICE_VERSION}"
                                        
                                        echo "Completed deployment of ${app}"
                                        """
                                    } 
                                }   
                                echo "Deployment successful"
                            }
                        }
                    }
                }
            }
            """)
        }
    }
} 