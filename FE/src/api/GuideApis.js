import { customAxios } from "./customAxios";

export const getGuideList = async () => {
  try {
    const response = await customAxios.get(`guides?limit=10&offset=1`, {});
    return response.data;
  } catch (error) {
    console.error("Error fetching guide list:", error);
    throw error;
  }
};

export const guideUpload = async (formData) => {
  delete customAxios.defaults.headers["Content-Type"];
  try {
    const response = await customAxios.post(
      `guides/file`,

      formData
    );
    return response.data;
  } catch (error) {
    console.error("Error fetching guide list:", error);
    throw error;
  }
};

export const getGuideDetail = async (guideId) => {
  try {
    const response = await customAxios.get(`guides/${guideId}`, {});
    return response.data;
  } catch (error) {
    console.error("Error fetching guide detail:", error);
    throw error;
  }
};

export const searchTitle = async (title) => {
  try {
    const response = await customAxios.get(
      `guides?limit=10&offset=1&title=${title}`
    );
    return response.data;
  } catch (error) {
    console.error("Error fetching guide list:", error);
    throw error;
  }
};

export const postFeedback = async (guideId, feedbackData) => {
  try {
    const response = await customAxios.post(
      `guides/${guideId}/feedback`,
      feedbackData
    );
    return response.data;
  } catch (error) {
    console.error("Error posting feedback:", error);
    throw error;
  }
};
