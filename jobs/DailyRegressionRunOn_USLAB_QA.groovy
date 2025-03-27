pipelineJob('DailyRegressionRunOn_USLAB_QA') {
    description('API')
    environment {
        TZ = 'Asia/Kolkata'  // Set the timezone globally for the pipeline
    }
    properties {
        pipelineTriggers {
            triggers {
                cron {
                    spec('H 20 * * *')  // Runs at 8:00 PM (20:00) every day in the Asia/Kolkata timezone
                }
            }
        }
    }
    parameters {
        string(name: 'Include_Modules' , defaultValue: '' , description: '')
        string(name: 'Exclude_Modules' , defaultValue: ['Usecases','Extensions'] , description: '')
        string(name: 'report_type' , defaultValue: 'Regression' , description: 'Value Could be Regression, Usecase, Extension')
    }
   // options {
   //     concurrency()  // Allow unlimited concurrent builds
   // }
    definition {
        cps {
            script('''
                pipeline {
                agent {
                    kubernetes {
                        label 'qa-jenkins'
                        defaultContainer 'jnlp'
                    }
                }
                stages {
                    stage('Clone Repository') {
                        steps {
                            git branch: '*/develop', url: 'https://devops_build2@bitbucket.org/syncappinc/krista-pytest-automation.git'
                            credentials('bitbucket-creds')
                        }
                    }
                    stage('Build') {
                        steps {
                            sh '''
                                cd /home/automation/krista-pytest-automation
                                cd "${WORKSPACE}"
                                touch propsfile
                                export PYTHONPATH=.
                                Select_POD="config_us_lab_qa.cfg"
                                if [ "${Include_Modules}" != "" ]
                                then
                                    inc_moduels="-modules ${Include_Modules}"
                                fi
                                if [ "${Exclude_Modules}" != "" ]
                                then
                                    exc_moduels="-exclude_modules ${Exclude_Modules}"
                                fi
                                python3 bin/run.py -config ${Select_POD} -report_type "${report_type}" ${inc_moduels} ${exc_moduels} 

                                export $(grep -v '^#' "${WORKSPACE}/propsfile" | xargs)
                                cp $FOLDER/$FILE "${WORKSPACE}"
                            '''
                        }
                    }
                }    
                    post {
                        always {
                            echo "Build finished. Performing post-build actions."
                        }
                        success {
                            echo "Build was successful. Publishing HTML report..."
                            publishHTML([                       // Publish HTML report as a post-build action
                                allowMissing: false,
                                alwaysLinkToLastBuild: true,
                                keepAll: true,
                                reportDir: "${env.FOLDER}",  // Directory to archive, e.g., ${FOLDER}
                                reportFiles: "${env.FILE}",  // Index page(s), e.g., ${FILE}
                                reportName: 'HTML Report',  // Title of the HTML report 
                                includes : "**/*.html" 
                                numberOfWorkers : 0                      
                            ])
                            emailext(
                                subject: "API ${report_type}-${SUBJECT} (Env.: USLAB-QA)"
                                bopdy: """
                                    <html>
                                        <body>
                                        <p>Hi All,</p>

                                        <BR><BR>
                                        Please find attached the Automation Report for Build # ${BUILD_VERSION}.

                                        <p><h4>Report Summary:</h4></p>
                                            <table cellspacing="0" cellpadding="4" border="1" align="left">
                                                <thead>
                                                <tr bgcolor="#F3F3F3">
                                                    <td><b>Type</b></td>
                                                    <td><b>Passed</b></td>
                                                    <td><b>Failed</b></td>
                                                    <td><b>Total</b></td>
                                                </tr>
                                                </thead>
                                                <tbody>
                                                <tr><td><b>${report_type} Test cases</b></td>
                                                    <td style="color:green;text-align: center;">${TOTAL_PASSED}</td>
                                                    <td style="color:red;text-align: center;">${TOTAL_FAILED}</td>
                                                <td style="text-align: center;">${TOTAL_TEST}</td>
                                                </tr>
                                                </tbody>
                                            </table>

                                        <BR>


                                        <BR><BR><BR>
                                        Thanks,<BR>
                                        QA Team,
                                from: 'service.automation@kristasoft.com'
                                to: qa@kristasoft.com,dev@kristasoft.com,devops@kristasoft.com,
                                attachmentPattern: 
                                mimeType: [
                                    'text/html',
                                    'text/plain'
                                ],
                                attachments: "${FILE}",
                                attachLog: false,
                                preSendScript: '$DEFAULT_PRESEND_SCRIPT',       // Use default pre-send script
                                postSendScript: '$DEFAULT_POSTSEND_SCRIPT',     // Use default post-send script
                                recipients: 'sagar.kedar@kristasoft.com',    // Send to a list of recipients
                                trigger: 'always'  // Always trigger the email to send
                            )
                        }
                        failure {
                            echo "Build failed. No HTML report will be published."
                        }
                    }
                }
            ''')
            }
        }
}