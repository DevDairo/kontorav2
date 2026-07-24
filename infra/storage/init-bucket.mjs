const required = (name) => {
  const value = process.env[name]?.trim();
  if (!value) {
    throw new Error(`Falta la variable ${name}`);
  }
  return value;
};

const apiUrl = required("STORAGE_API_URL").replace(/\/+$/, "");
const serviceRoleKey = required("STORAGE_SERVICE_ROLE_KEY");
const bucket = required("STORAGE_BUCKET");
const fileSizeLimit = Number(required("STORAGE_BUCKET_FILE_SIZE_LIMIT"));

if (!Number.isSafeInteger(fileSizeLimit) || fileSizeLimit <= 0) {
  throw new Error("STORAGE_BUCKET_FILE_SIZE_LIMIT debe ser un entero positivo");
}

const headers = {
  Authorization: `Bearer ${serviceRoleKey}`,
  apikey: serviceRoleKey,
  "Content-Type": "application/json",
};
const bucketUrl = `${apiUrl}/bucket/${encodeURIComponent(bucket)}`;
const bucketPolicy = {
  public: false,
  file_size_limit: fileSizeLimit,
  allowed_mime_types: ["image/*", "application/pdf"],
};

const responseText = async (response) => {
  const body = await response.text();
  return body.length > 500 ? `${body.slice(0, 500)}...` : body;
};

const isNotFound = (response, body) =>
  response.status === 404 ||
  (response.status === 400 &&
    (body.includes('"statusCode":"404"') ||
      body.includes('"statusCode":404') ||
      body.includes('"error":"not_found"')));

const getResponse = await fetch(bucketUrl, { headers });
const getBody = await responseText(getResponse);

if (isNotFound(getResponse, getBody)) {
  const createResponse = await fetch(`${apiUrl}/bucket`, {
    method: "POST",
    headers,
    body: JSON.stringify({
      id: bucket,
      name: bucket,
      ...bucketPolicy,
    }),
  });
  const createBody = await responseText(createResponse);

  if (!createResponse.ok && createResponse.status !== 409) {
    throw new Error(
      `No fue posible crear el bucket (HTTP ${createResponse.status}): ${createBody}`,
    );
  }
} else if (!getResponse.ok) {
  throw new Error(
    `No fue posible consultar el bucket (HTTP ${getResponse.status}): ${getBody}`,
  );
}

const updateResponse = await fetch(bucketUrl, {
  method: "PUT",
  headers,
  body: JSON.stringify(bucketPolicy),
});
const updateBody = await responseText(updateResponse);

if (!updateResponse.ok) {
  throw new Error(
    `No fue posible asegurar la configuracion privada del bucket (HTTP ${updateResponse.status}): ${updateBody}`,
  );
}

console.log(`Bucket privado '${bucket}' preparado correctamente.`);
