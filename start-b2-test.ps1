$env:SPRING_PROFILES_ACTIVE='local'
$env:B2_ENABLED='true'
$env:B2_KEY_ID='a5352921f096'
$env:B2_APPLICATION_KEY='005c6bc553616bb03bc4e6b6bfdf94d3d5941dc78a'
$env:B2_BUCKET='itaccess-storage'
$env:B2_ENDPOINT='https://s3.us-east-005.backblazeb2.com'
$env:B2_REGION='us-east-005'
$env:B2_DOCUMENTS_PREFIX='document-archive/'
mvn spring-boot:run
