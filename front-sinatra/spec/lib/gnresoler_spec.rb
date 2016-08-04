describe GnResolver do
  describe ".version" do
    it "returns project's version" do
      expect(subject.version).to match(/^\d+\.\d+\.\d+/)
    end
  end
end
