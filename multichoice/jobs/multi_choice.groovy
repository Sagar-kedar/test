pipelineJob('MULTI_CHOICE') {
definition {
    cps {
    script(
        pipeline {
        agent any
        parameters {
        // Active choice parameter to select multiple services
        activeChoiceParam('SERVICE_NAME') {
            description('Select the service name(s) for deployment')
            filterable()  // Allows filtering in the UI
            choiceType('MULTI_SELECT')  // Multi-select checkbox
            groovyScript {
                script('return ["accesspoint3-21", "studio", "proxy", "client", "krista-ai-server", "client-beta", "platform", "accesspoint3"]')
            }
        }
        // Other parameters
        string(name: 'HELM_VERSION', defaultValue: 'v3.0.0', description: 'Version of Helm to deploy')
        string(name: 'NAMESPACE', defaultValue: 'default', description: 'Kubernetes namespace')
        }
        stages {
            stage('Deploy Helm Charts') {
                steps {
                    script {
                        // Get the selected services
                        def selectedServices = params.SERVICE_NAME
                        
                        // Print the selected services
                        echo "Selected services: ${selectedServices}"
                        
                        // Deploy Helm charts based on the selected services
                        if (selectedServices.contains("accesspoint3-21")) {
                            echo "Deploying Helm chart for accesspoint3-21"
                        }
                        if (selectedServices.contains("studio")) {
                            echo "Deploying Helm chart for studio"
                            
                        }
                        if (selectedServices.contains("proxy")) {
                            echo "Deploying Helm chart for proxy"
                            
                        }
                        if (selectedServices.contains("client")) {
                            echo "Deploying Helm chart for client"
                            
                        }
                        if (selectedServices.contains("krista-ai-server")) {
                            echo "Deploying Helm chart for krista-ai-server"
                            
                        }
                        if (selectedServices.contains("client-beta")) {
                            echo "Deploying Helm chart for client-beta"
                        
                        }
                        if (selectedServices.contains("platform")) {
                            echo "Deploying Helm chart for platform"
                            
                        }
                        if (selectedServices.contains("accesspoint3")) {
                            echo "Deploying Helm chart for accesspoint3"
                            
                        }
                    }
                }
            }
        }
        }
    )    
    }
}
}
