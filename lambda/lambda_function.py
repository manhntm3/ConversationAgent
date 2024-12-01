
import json
import boto3

# Bedrock client used to interact with APIs around models
bedrock = boto3.client(
    service_name='bedrock',
    region_name='us-east-1'
)

# Bedrock Runtime client used to invoke and question the models
bedrock_runtime = boto3.client(
    service_name='bedrock-runtime',
    region_name='us-east-1'
)

def lambda_handler(event, context):
    # Just shows an example of how to retrieve information about available models
    # prompt = json.loads(event.get("body")).get("input")
    prompt = event.get("prompt")

    # The payload to be provided to Bedrock 
    # body = json.dumps({
    #     "{"prompt": "\n\nHuman: story of two dogs\n\nAssistant:", "max_tokens_to_sample" : 300}""
    # })
    formatted_prompt = f"""
    <|begin_of_text|><|start_header_id|>user<|end_header_id|>
    {prompt}
    <|eot_id|>
    <|start_header_id|>assistant<|end_header_id|>
    """

    # Format the request payload using the model's native structure.
    native_request = {
        "prompt": formatted_prompt,
        "max_gen_len": 64,
        "temperature": 0.5,
    }

    # The actual call to retrieve an answer from the model
    response = bedrock_runtime.invoke_model(
        body = json.dumps(native_request),
        modelId="us.meta.llama3-2-1b-instruct-v1:0",
        accept='application/json',
        contentType='application/json'
    )

    response_body = json.loads(response.get('body').read())
    generation_content = response_body['generation']
    
    return {
        'statusCode': 200,
        'body': json.dumps({"answer": generation_content})
    }

